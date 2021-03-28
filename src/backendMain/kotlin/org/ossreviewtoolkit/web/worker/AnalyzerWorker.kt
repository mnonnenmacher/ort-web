/*
 * Copyright (C) 2021 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package org.ossreviewtoolkit.web.worker

import java.io.File
import java.util.UUID

import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

import org.ossreviewtoolkit.analyzer.Analyzer
import org.ossreviewtoolkit.analyzer.PackageManager
import org.ossreviewtoolkit.downloader.DownloadException
import org.ossreviewtoolkit.downloader.Downloader
import org.ossreviewtoolkit.model.Package
import org.ossreviewtoolkit.model.SourceCodeOrigin
import org.ossreviewtoolkit.model.VcsInfo
import org.ossreviewtoolkit.model.VcsType
import org.ossreviewtoolkit.model.config.AnalyzerConfiguration
import org.ossreviewtoolkit.model.config.DownloaderConfiguration
import org.ossreviewtoolkit.utils.collectMessagesAsString
import org.ossreviewtoolkit.utils.log
import org.ossreviewtoolkit.utils.ortDataDirectory
import org.ossreviewtoolkit.utils.safeMkdirs
import org.ossreviewtoolkit.utils.showStackTrace
import org.ossreviewtoolkit.web.model.AnalyzerRunDao
import org.ossreviewtoolkit.web.model.AnalyzerRunStatus
import org.ossreviewtoolkit.web.model.AnalyzerRuns
import org.ossreviewtoolkit.web.model.AnalyzerRunsPackages
import org.ossreviewtoolkit.web.model.PackageDao
import org.ossreviewtoolkit.web.model.Packages
import org.ossreviewtoolkit.web.model.ScanJobDao
import org.ossreviewtoolkit.web.model.ScanJobStatus

class AnalyzerWorker : Thread("AnalyzerWorker") {
    companion object {
        private const val POLL_INTERVAL = 5000L

        private val DOWNLOADER_CONFIGURATION = DownloaderConfiguration(
            sourceCodeOrigins = listOf(SourceCodeOrigin.VCS)
        )

    }

    override fun run() {
        while (true) {
            sleep(POLL_INTERVAL)

            try {
                val analyzerRun = findAnalyzerRun()
                if (analyzerRun != null) {
                    val downloadResult = download(analyzerRun)
                    if (downloadResult.success) {
                        log.info { "Finished download of ${analyzerRun.detach()}." }
                        val analyzeResult = analyze(analyzerRun, downloadResult.downloadDir)
                        val analyzerRunDao = analyzeResult.analyzerRun
                        if (analyzeResult.success && analyzerRunDao != null) {
                            log.info { "Finished analysis of ${analyzerRun.detach().id}." }
                            transaction { analyzerRun.status = AnalyzerRunStatus.SUCCESS }
                            analyzerRunDao.analyzerRun.result.projects.forEach {
                                scheduleScan(analyzerRunDao, it.toPackage())
                            }
                            analyzerRunDao.analyzerRun.result.packages.forEach {
                                scheduleScan(analyzerRunDao, it.pkg)
                            }
                        } else {
                            // TODO: Add analyzer result with error message.
                            log.warn { "Analysis of ${analyzerRun.detach()} failed: ${analyzeResult.message}" }
                            transaction { analyzerRun.status = AnalyzerRunStatus.FAILED }
                        }
                    } else {
                        // TODO: Add analyzer result with error message.
                        log.warn { "Download of ${analyzerRun.detach()} failed: ${downloadResult.message}." }
                        transaction { analyzerRun.status = AnalyzerRunStatus.FAILED }
                    }
                    // TODO: Enable deletion of download directory.
                    //downloadResult.downloadDir.safeDeleteRecursively()
                }
            } catch (e: Exception) {
                e.showStackTrace()
            }
        }
    }

    private fun findAnalyzerRun(): AnalyzerRunDao? =
        transaction {
            AnalyzerWorker.log.debug { "Searching for queued analyzer run." }

            AnalyzerRunDao.find { AnalyzerRuns.status eq AnalyzerRunStatus.QUEUED }.limit(1).firstOrNull()
                .also {
                    if (it != null) {
                        AnalyzerWorker.log.debug { "Found queued analyzer run ${it.id}." }
                        it.status = AnalyzerRunStatus.ANALYZING_DEPENDENCIES
                    } else {
                        AnalyzerWorker.log.debug { "No queued analyzer run found." }
                    }
                }
        }

    private fun download(analyzerRun: AnalyzerRunDao): DownloadPhaseResult {
        log.info { "Starting download of $analyzerRun." }
        transaction { analyzerRun.status = AnalyzerRunStatus.DOWNLOADING_SOURCE_CODE }
        val downloadDir = ortDataDirectory.resolve("web/download/${analyzerRun.id}")
        if (downloadDir.exists()) {
            return DownloadPhaseResult(
                false,
                downloadDir,
                "Download directory ${downloadDir.absolutePath} already exists."
            )
        }

        downloadDir.safeMkdirs()

        val vcs = transaction {
            val projectRepository = analyzerRun.projectRepository

            VcsInfo(
                type = VcsType(projectRepository.repository.type.name),
                url = projectRepository.repository.url,
                revision = analyzerRun.revision,
                path = projectRepository.path
            )
        }

        val pkg = Package.EMPTY.copy(
            vcs = vcs,
            vcsProcessed = vcs.normalize()
        )

        return try {
            Downloader(DOWNLOADER_CONFIGURATION).download(pkg, downloadDir, true)
            DownloadPhaseResult(true, downloadDir)
        } catch (e: DownloadException) {
            e.showStackTrace()
            DownloadPhaseResult(false, downloadDir, e.collectMessagesAsString())
        }
    }

    private fun analyze(analyzerRunDao: AnalyzerRunDao, downloadDir: File): AnalyzerPhaseResult {
        log.info { "Starting analysis of $analyzerRunDao." }
        transaction { analyzerRunDao.status = AnalyzerRunStatus.ANALYZING_DEPENDENCIES }

        val config = AnalyzerConfiguration(ignoreToolVersions = true, allowDynamicVersions = true)
        val analyzer = Analyzer(config)

        return try {
            val packageManagers = PackageManager.ALL.filter { it.managerName in listOf("NPM", "Gradle", "Maven") }
            val ortResult = analyzer.analyze(downloadDir.normalize(), packageManagers)
            val analyzerRun = ortResult.analyzer

            if (analyzerRun != null) {
                transaction {
                    analyzerRunDao.analyzerRun = analyzerRun
                }
                AnalyzerPhaseResult(true, analyzerRunDao)
            } else {
                AnalyzerPhaseResult(false, null)
            }
        } catch (e: Exception) {
            e.showStackTrace()
            AnalyzerPhaseResult(false, null, e.collectMessagesAsString())
        }
    }

    private fun scheduleScan(analyzerRunDao: AnalyzerRunDao, pkg: Package) {
        transaction {
            val packageDao = PackageDao.find { Packages.packageId eq pkg.id.toCoordinates() }
                .firstOrNull { it.pkg == pkg } ?: PackageDao.new(UUID.randomUUID()) {
                packageId = pkg.id
                this.pkg = pkg
            }

            AnalyzerRunsPackages.insert {
                it[analyzerRun] = analyzerRunDao.id
                it[this.pkg] = packageDao.id
            }

            if (packageDao.scannerRuns.empty()) {
                ScanJobDao.new {
                    this.pkg = packageDao
                    status = ScanJobStatus.QUEUED
                }
            }
        }
    }

    private data class DownloadPhaseResult(
        val success: Boolean,
        val downloadDir: File,
        val message: String = ""
    )

    private data class AnalyzerPhaseResult(
        val success: Boolean,
        val analyzerRun: AnalyzerRunDao?,
        val message: String = ""
    )
}
