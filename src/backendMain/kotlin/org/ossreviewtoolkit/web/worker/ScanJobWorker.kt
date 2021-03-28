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

import java.util.UUID

import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

import org.ossreviewtoolkit.model.ArtifactProvenance
import org.ossreviewtoolkit.model.RemoteArtifact
import org.ossreviewtoolkit.model.RepositoryProvenance
import org.ossreviewtoolkit.model.VcsInfo
import org.ossreviewtoolkit.model.jsonMapper
import org.ossreviewtoolkit.model.utils.rawParam
import org.ossreviewtoolkit.utils.collectMessagesAsString
import org.ossreviewtoolkit.utils.log
import org.ossreviewtoolkit.utils.showStackTrace
import org.ossreviewtoolkit.web.model.PackagesScannerRuns
import org.ossreviewtoolkit.web.model.ScanJobDao
import org.ossreviewtoolkit.web.model.ScanJobStatus
import org.ossreviewtoolkit.web.model.ScanJobs
import org.ossreviewtoolkit.web.model.ScannerRunDao
import org.ossreviewtoolkit.web.model.ScannerRunStatus
import org.ossreviewtoolkit.web.model.ScannerRuns
import org.ossreviewtoolkit.web.sizedListOf

class ScanJobWorker : Thread("ScanJobWorker") {
    companion object {
        private const val POLL_INTERVAL = 5000L
    }

    override fun run() {
        sleep(POLL_INTERVAL)

        while (true) {
            try {
                val scanJob = findScanJob()
                if (scanJob != null) {
                    val result = processJob(scanJob)
                    when {
                        result.success -> log.info { "Scan job for '${scanJob.id}' processed." }
                        else -> log.warn {
                            "Processing of scan job for '${scanJob.id}' failed: " +
                                    result.message
                        }
                    }
                } else {
                    sleep(POLL_INTERVAL)
                }
            } catch (e: Exception) {
                e.showStackTrace()
            }
        }
    }

    private fun findScanJob(): ScanJobDao? =
        transaction {
            this@ScanJobWorker.log.debug { "Searching for queued scan job..." }

            ScanJobDao.find { ScanJobs.status eq ScanJobStatus.QUEUED }.limit(1).firstOrNull().also {
                if (it != null) {
                    it.status = ScanJobStatus.IN_PROGRESS
                    this@ScanJobWorker.log.debug { "Found scan job ${it.id}." }
                } else {
                    this@ScanJobWorker.log.debug { "No scan job found." }
                }
            }?.also {
                it.load(ScanJobDao::pkg)
            }
        }

    private fun processJob(scanJob: ScanJobDao): ScanPhaseResult {
        return try {
            transaction {
                addLogger(StdOutSqlLogger)

                this@ScanJobWorker.log.info { "Starting scan of ${scanJob.pkg.packageId.toCoordinates()}." }

                val provenance = scanJob.pkg.pkg.vcsProcessed.takeIf { it != VcsInfo.EMPTY }?.let {
                    RepositoryProvenance(vcsInfo = it.copy(path = ""))
                } ?: scanJob.pkg.pkg.sourceArtifact.takeIf { it != RemoteArtifact.EMPTY }?.let {
                    ArtifactProvenance(sourceArtifact = it)
                } ?: return@transaction ScanPhaseResult(false, "No provenance found.")

                val existingScannerRun = when (provenance) {
                    is ArtifactProvenance -> ScannerRunDao.find {
                        rawParam("${ScannerRuns.provenance.name}->'source_artifact'") eq
                                rawParam("to_jsonb('${jsonMapper.writeValueAsString(provenance.sourceArtifact)}')")
                    }

                    is RepositoryProvenance -> ScannerRunDao.find {
                        rawParam("${ScannerRuns.provenance.name}->'vcs_info'") eq
                                rawParam("to_jsonb('${jsonMapper.writeValueAsString(provenance.vcsInfo)}'::text)")
                    }
                }.limit(1).firstOrNull()

                if (existingScannerRun != null) {
                    if (scanJob.pkg !in existingScannerRun.packages) {
                        PackagesScannerRuns.insert {
                            it[pkg] = scanJob.pkg.id
                            it[scannerRun] = existingScannerRun.id
                        }
                    }
                } else {
                    val scannerRun = ScannerRunDao.new(UUID.randomUUID()) {
                        timestamp = System.currentTimeMillis()
                        this.provenance = provenance
                        status = ScannerRunStatus.QUEUED
                        packages = sizedListOf(scanJob.pkg)
                    }

                    PackagesScannerRuns.insert {
                        it[pkg] = scanJob.pkg.id
                        it[PackagesScannerRuns.scannerRun] = scannerRun.id
                    }
                }

                ScanPhaseResult(true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ScanPhaseResult(false, e.collectMessagesAsString())
        } finally {
            transaction { scanJob.delete() }
        }
    }

    private data class ScanPhaseResult(
        val success: Boolean,
        val message: String = ""
    )
}