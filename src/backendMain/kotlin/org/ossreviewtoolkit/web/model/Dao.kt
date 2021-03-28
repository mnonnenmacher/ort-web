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

package org.ossreviewtoolkit.web.model

import java.util.UUID

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Table

import org.ossreviewtoolkit.model.AnalyzerRun as OrtAnalyzerRun
import org.ossreviewtoolkit.model.Identifier as OrtIdentifier
import org.ossreviewtoolkit.model.KnownProvenance as OrtKnownProvenance
import org.ossreviewtoolkit.model.Package as OrtPackage
import org.ossreviewtoolkit.model.ScanSummary as OrtScanSummary
import org.ossreviewtoolkit.model.ScannerDetails as OrtScannerDetails
import org.ossreviewtoolkit.scanner.storages.utils.jsonb
import org.ossreviewtoolkit.web.toWeb

object Projects : UUIDTable() {
    val name = text("name").uniqueIndex()
}

class ProjectDao(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ProjectDao>(Projects)

    var name by Projects.name

    val projectRepositories by ProjectRepositoryDao referrersOn ProjectRepositories.project

    fun detach(): WebProject = WebProject(id.value.toString(), name)
}

object ProjectRepositories : UUIDTable() {
    // TODO: Add name.
    val path = text("path")

    val project = reference("project", Projects)
    val repository = reference("repository", Repositories)
}

class ProjectRepositoryDao(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ProjectRepositoryDao>(ProjectRepositories)

    var path by ProjectRepositories.path

    var project by ProjectDao referencedOn ProjectRepositories.project
    var repository by RepositoryDao referencedOn ProjectRepositories.repository

    val analyzerRuns by AnalyzerRunDao referrersOn AnalyzerRuns.projectRepository

    fun detach(): ProjectRepository = ProjectRepository(id.value.toString(), path, repository.detach())
}

object Repositories : UUIDTable() {
    val type = text("type")
    val url = text("url")

    init {
        uniqueIndex(type, url)
    }
}

class RepositoryDao(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<RepositoryDao>(Repositories)

    var type: RepositoryType by Repositories.type.transform({ it.name }, { RepositoryType.valueOf(it) })
    var url by Repositories.url

    val projectRepositories by ProjectRepositoryDao referrersOn ProjectRepositories.repository

    fun detach(): Repository = Repository(id.value.toString(), type, url)
}

object AnalyzerRuns : UUIDTable() {
    val timestamp = long("timestamp")
    val revision = text("revision")
    val reference = text("reference")
    val status = enumerationByName("status", 50, AnalyzerRunStatus::class)
    val analyzerRun = jsonb("analyzer_run", OrtAnalyzerRun::class)

    val projectRepository = reference("project_repository", ProjectRepositories)
}

class AnalyzerRunDao(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<AnalyzerRunDao>(AnalyzerRuns)

    var timestamp by AnalyzerRuns.timestamp
    var revision by AnalyzerRuns.revision
    var reference by AnalyzerRuns.reference
    var status by AnalyzerRuns.status
    var analyzerRun by AnalyzerRuns.analyzerRun

    val packages by PackageDao via AnalyzerRunsPackages
    var projectRepository by ProjectRepositoryDao referencedOn AnalyzerRuns.projectRepository

    fun detach(): AnalyzerRun = AnalyzerRun(
        id.value.toString(),
        timestamp,
        revision,
        reference,
        status,
        createProjects(analyzerRun)
    )

    private fun createProjects(analyzerRun: OrtAnalyzerRun): List<Project> {
        return analyzerRun.result.projects.map { it.toWeb() }
    }
}

object Packages : UUIDTable() {
    val packageId = text("package_id")
    val pkg = jsonb("package", OrtPackage::class).uniqueIndex()
}

class PackageDao(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<PackageDao>(Packages)

    var packageId by Packages.packageId.transform({ it.toCoordinates() }, { OrtIdentifier(it) })
    var pkg by Packages.pkg

    val analyzerRuns by AnalyzerRunDao via AnalyzerRunsPackages
    val scannerRuns by ScannerRunDao via PackagesScannerRuns
    val scanJobs by ScanJobDao referrersOn ScanJobs.pkg

    fun detach(): Package {
        val lastScannerRun = scannerRuns.lastOrNull()

        return Package(
            id = id.toString(),
            identifier = packageId.toWeb(),
            authors = pkg.authors,
            declaredLicenses = pkg.declaredLicensesProcessed.allLicenses.toSortedSet(),
            concludedLicense = pkg.concludedLicense?.toString().orEmpty(),
            detectedLicenses = lastScannerRun?.summary?.licenseFindings?.mapTo(sortedSetOf()) { it.license.toString() }
                .orEmpty(),
            sourceArtifact = pkg.sourceArtifact.toWeb(),
            vcsInfo = pkg.vcsProcessed.toWeb(),
            lastScanStatus = lastScannerRun?.status,
            lastScanId = lastScannerRun?.id?.toString()
        )
    }
}

object AnalyzerRunsPackages : Table() {
    val analyzerRun = reference("analyzer_run", AnalyzerRuns)
    val pkg = reference("package", Packages)
}

object ScanJobs : UUIDTable() {
    val status = enumerationByName("status", 50, ScanJobStatus::class)

    val pkg = reference("package", Packages)
}

class ScanJobDao(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ScanJobDao>(ScanJobs)

    var status by ScanJobs.status

    var pkg by PackageDao referencedOn ScanJobs.pkg
}

object ScannerRuns : UUIDTable() {
    val timestamp = long("timestamp")
    val provenance = jsonb("provenance", OrtKnownProvenance::class)
    val scanner = jsonb("scanner", OrtScannerDetails::class).nullable()
    val summary = jsonb("summary", OrtScanSummary::class).nullable()
    val status = enumerationByName("status", 50, ScannerRunStatus::class)
}

class ScannerRunDao(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ScannerRunDao>(ScannerRuns)

    var timestamp by ScannerRuns.timestamp
    var provenance by ScannerRuns.provenance
    var scanner by ScannerRuns.scanner
    var summary by ScannerRuns.summary
    var status by ScannerRuns.status

    var packages by PackageDao via PackagesScannerRuns

    fun detach(): ScannerRun = ScannerRun(
        id = id.toString(),
        timestamp = timestamp,
        provenance = provenance.toString(),
        scanner = scanner?.toString().orEmpty(),
        status = status
    )
}

object PackagesScannerRuns : Table() {
    val pkg = reference("package", Packages)
    val scannerRun = reference("scanner_run", ScannerRuns)
}
