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

import java.time.Instant

import org.jetbrains.exposed.sql.transactions.transaction

import org.ossreviewtoolkit.model.ArtifactProvenance
import org.ossreviewtoolkit.model.RepositoryProvenance
import org.ossreviewtoolkit.model.ScanSummary
import org.ossreviewtoolkit.model.config.DownloaderConfiguration
import org.ossreviewtoolkit.model.config.ScannerConfiguration
import org.ossreviewtoolkit.model.createAndLogIssue
import org.ossreviewtoolkit.scanner.scanners.scancode.ScanCode
import org.ossreviewtoolkit.utils.collectMessagesAsString
import org.ossreviewtoolkit.utils.log
import org.ossreviewtoolkit.utils.ortDataDirectory
import org.ossreviewtoolkit.utils.showStackTrace
import org.ossreviewtoolkit.web.model.ScannerRunDao
import org.ossreviewtoolkit.web.model.ScannerRunStatus
import org.ossreviewtoolkit.web.model.ScannerRuns
import org.ossreviewtoolkit.model.Package as OrtPackage

class ScannerWorker : Thread("ScannerWorker") {
    companion object {
        private const val POLL_INTERVAL = 5000L
    }

    override fun run() {
        while (true) {
            sleep(POLL_INTERVAL)

            try {
                val scannerRun = findScannerRun()
                if (scannerRun != null) {
                    val result = scan(scannerRun)
                    when {
                        result.success -> log.info { "Scan '${scannerRun.id}' finished." }
                        else -> log.warn { "Scan '${scannerRun.id}' failed: ${result.message}" }
                    }
                }
            } catch (e: Exception) {
                e.showStackTrace()
            }
        }
    }

    private fun findScannerRun(): ScannerRunDao? =
        transaction {
            this@ScannerWorker.log.debug { "Searching for queued scanner run..." }

            ScannerRunDao.find { ScannerRuns.status eq ScannerRunStatus.QUEUED }.limit(1).firstOrNull().also {
                if (it != null) {
                    this@ScannerWorker.log.debug { "Found scanner run ${it.id}." }
                    it.status = ScannerRunStatus.SCANNING
                } else {
                    this@ScannerWorker.log.debug { "No scanner run found." }
                }
            }
        }

    private fun scan(scannerRun: ScannerRunDao): ScanPhaseResult {
        log.info { "Starting scan '${scannerRun.id}' for provenance ${scannerRun.provenance}." }

        val scanner = ScanCode.Factory().create(ScannerConfiguration(), DownloaderConfiguration())

        val downloadDir = ortDataDirectory.resolve("web/download-pkg/${scannerRun.id}")
        val outputDir = createTempDir("scancode")
        outputDir.deleteOnExit()

        val pkg = when (val provenance = scannerRun.provenance) {
            is ArtifactProvenance -> OrtPackage.EMPTY.copy(sourceArtifact = provenance.sourceArtifact)
            is RepositoryProvenance -> OrtPackage.EMPTY.copy(vcsProcessed = provenance.vcsInfo)
        }

        return try {
            val scanResult = scanner.scanPackage(
                scanner.details,
                pkg,
                outputDir,
                downloadDir
            )

            transaction {
                scannerRun.scanner = scanResult.scanner
                scannerRun.summary = scanResult.summary
                scannerRun.status = ScannerRunStatus.SUCCESS
            }

            ScanPhaseResult(true)
        } catch (e: Exception) {
            e.showStackTrace()
            val message = "Could not scan '${scannerRun.id}': ${e.collectMessagesAsString()}"

            log.warn { message }

            val issue = createAndLogIssue(
                source = scanner.scannerName,
                message = "Scan failed: ${e.collectMessagesAsString()}"
            )

            val now = Instant.now()

            val failedSummary = ScanSummary(
                startTime = now,
                endTime = now,
                fileCount = 0,
                packageVerificationCode = "",
                licenseFindings = sortedSetOf(),
                copyrightFindings = sortedSetOf(),
                issues = listOf(issue)
            )

            transaction {
                scannerRun.status = ScannerRunStatus.FAILED
                scannerRun.scanner = scanner.details
                scannerRun.summary = failedSummary
            }

            ScanPhaseResult(false, message)
        }
    }

    private data class ScanPhaseResult(
        val success: Boolean,
        val message: String = ""
    )
}
