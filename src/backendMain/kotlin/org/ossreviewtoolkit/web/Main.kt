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

package org.ossreviewtoolkit.web

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.routing.*
import io.kvision.remote.applyRoutes
import io.kvision.remote.kvisionInit

import java.util.UUID

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SchemaUtils.withDataBaseLock
import org.jetbrains.exposed.sql.transactions.transaction

import org.ossreviewtoolkit.model.config.PostgresStorageConfiguration
import org.ossreviewtoolkit.model.utils.DatabaseUtils
import org.ossreviewtoolkit.utils.ortConfigDirectory
import org.ossreviewtoolkit.utils.printStackTrace
import org.ossreviewtoolkit.web.config.OrtWebConfiguration
import org.ossreviewtoolkit.web.model.AnalyzerRuns
import org.ossreviewtoolkit.web.model.AnalyzerRunsPackages
import org.ossreviewtoolkit.web.model.Packages
import org.ossreviewtoolkit.web.model.PackagesScannerRuns
import org.ossreviewtoolkit.web.model.ProjectDao
import org.ossreviewtoolkit.web.model.ProjectRepositories
import org.ossreviewtoolkit.web.model.ProjectRepositoryDao
import org.ossreviewtoolkit.web.model.Projects
import org.ossreviewtoolkit.web.model.Repositories
import org.ossreviewtoolkit.web.model.RepositoryDao
import org.ossreviewtoolkit.web.model.RepositoryType
import org.ossreviewtoolkit.web.model.ScanJobs
import org.ossreviewtoolkit.web.model.ScannerRuns
import org.ossreviewtoolkit.web.service.AnalyzerServiceManager
import org.ossreviewtoolkit.web.service.PackageServiceManager
import org.ossreviewtoolkit.web.service.ProjectServiceManager
import org.ossreviewtoolkit.web.service.ScannerServiceManager
import org.ossreviewtoolkit.web.worker.AnalyzerWorker
import org.ossreviewtoolkit.web.worker.ScanJobWorker
import org.ossreviewtoolkit.web.worker.ScannerWorker

fun Application.main() {
    printStackTrace = true

    install(Compression)

    routing {
        applyRoutes(AnalyzerServiceManager)
        applyRoutes(PackageServiceManager)
        applyRoutes(ProjectServiceManager)
        applyRoutes(ScannerServiceManager)
    }

    kvisionInit()

    val config = OrtWebConfiguration.load(configFile = ortConfigDirectory.resolve("config/web.conf"))

    require(config.postgres != null) { "ORT config file is missing configuration for the PostgreSQL connection." }

    initDatabase(config.postgres)

    AnalyzerWorker().start()
    ScanJobWorker().start()
    ScannerWorker().start()
}

private fun initDatabase(config: PostgresStorageConfiguration) {
    val dataSource = DatabaseUtils.createHikariDataSource(config, "ORT-Web")

    Database.connect(dataSource)

    transaction {
        withDataBaseLock {
            SchemaUtils.createMissingTablesAndColumns(
                Projects,
                Repositories,
                ProjectRepositories,
                AnalyzerRuns,
                Packages,
                AnalyzerRunsPackages,
                ScanJobs,
                ScannerRuns,
                PackagesScannerRuns
            )
        }

        if (ProjectDao.all().count() == 0L) {
            val repository = RepositoryDao.new(UUID.randomUUID()) {
                type = RepositoryType.GIT
                url = "https://github.com/oss-review-toolkit/ort.git"
            }

            val project = ProjectDao.new(UUID.randomUUID()) {
                name = "ORT Test Projects"
            }

            ProjectRepositoryDao.new {
                path = "analyzer/src/funTest/assets/projects/synthetic/gradle"
                this.project = project
                this.repository = repository
            }

            ProjectRepositoryDao.new {
                path = "analyzer/src/funTest/assets/projects/synthetic/npm"
                this.project = project
                this.repository = repository
            }
        }
    }
}
