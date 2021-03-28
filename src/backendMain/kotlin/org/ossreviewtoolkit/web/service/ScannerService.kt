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

import org.jetbrains.exposed.sql.transactions.transaction

import org.ossreviewtoolkit.web.model.PackageDao
import org.ossreviewtoolkit.web.model.ScanSummary
import org.ossreviewtoolkit.web.model.ScannerRun
import org.ossreviewtoolkit.web.model.ScannerRunDao
import org.ossreviewtoolkit.web.toUUID
import org.ossreviewtoolkit.web.toWeb

actual class ScannerService : IScannerService {
    override suspend fun readScannerRuns(): List<ScannerRun> {
        return try {
            // TODO: Validation

            transaction {
                ScannerRunDao.all().map { it.detach() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw ServiceException("Could not read scanner runs: ${e.message}")
        }
    }

    override suspend fun readScannerRunsForPackage(packageId: String): List<ScannerRun> {
        return try {
            // TODO: Validation

            transaction {
                PackageDao.findById(packageId.toUUID())?.scannerRuns?.map {
                    it.detach()
                } ?: emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw ServiceException("Could not read analyzer runs: ${e.message}")
        }
    }

    override suspend fun readScanSummary(scannerRunId: String): ScanSummary {
        return try {
            // TODO: Validation

            transaction {
                ScannerRunDao.findById(scannerRunId.toUUID())?.summary?.toWeb()
                    ?: throw IllegalArgumentException("Scanner run '$scannerRunId' not found.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw ServiceException("Could not read scan summary: ${e.message}")
        }
    }
}
