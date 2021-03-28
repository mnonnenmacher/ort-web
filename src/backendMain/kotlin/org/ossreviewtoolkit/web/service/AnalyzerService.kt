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

package org.ossreviewtoolkit.web.service

import io.kvision.remote.ServiceException

import java.time.Instant

import org.jetbrains.exposed.sql.transactions.transaction

import org.ossreviewtoolkit.model.AnalyzerResult as OrtAnalyzerResult
import org.ossreviewtoolkit.model.AnalyzerRun as OrtAnalyzerRun
import org.ossreviewtoolkit.model.config.AnalyzerConfiguration
import org.ossreviewtoolkit.utils.Environment
import org.ossreviewtoolkit.web.model.AnalyzerRun
import org.ossreviewtoolkit.web.model.AnalyzerRunDao
import org.ossreviewtoolkit.web.model.AnalyzerRunStatus
import org.ossreviewtoolkit.web.model.PackageDao
import org.ossreviewtoolkit.web.model.ProjectRepositoryDao
import org.ossreviewtoolkit.web.toUUID

actual class AnalyzerService : IAnalyzerService {
    override suspend fun startAnalyzer(projectRepositoryId: String, revision: String) {
        try {
            // TODO: Validation
            transaction {
                val repository = ProjectRepositoryDao.findById(projectRepositoryId.toUUID())

                if (repository != null) {
                    AnalyzerRunDao.new {
                        timestamp = Instant.now().epochSecond
                        this.revision = revision
                        this.reference = ""
                        status = AnalyzerRunStatus.QUEUED
                        analyzerRun = OrtAnalyzerRun(
                            environment = Environment(),
                            config = AnalyzerConfiguration(),
                            result = OrtAnalyzerResult.EMPTY
                        )

                        this.projectRepository = repository
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw ServiceException("Could not start analyzer: ${e.message}")
        }
    }

    override suspend fun readAnalyzerRunsForPackage(packageId: String): List<AnalyzerRun> {
        return try {
            transaction {
                PackageDao.findById(packageId.toUUID())?.analyzerRuns?.map {
                    it.detach()
                } ?: emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw ServiceException("Could not read analyzer runs: ${e.message}")
        }
    }

    override suspend fun readAnalyzerRunsForProjectRepository(projectRepositoryId: String): List<AnalyzerRun> {
        return try {
            transaction {
                ProjectRepositoryDao.findById(projectRepositoryId.toUUID())?.analyzerRuns?.map {
                    it.detach()
                } ?: emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw ServiceException("Could not read analyzer runs: ${e.message}")
        }
    }
}
