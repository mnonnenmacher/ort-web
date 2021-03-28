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

import io.kvision.redux.RAction

import org.ossreviewtoolkit.web.model.AnalyzerRun
import org.ossreviewtoolkit.web.model.Package
import org.ossreviewtoolkit.web.model.ProjectRepository
import org.ossreviewtoolkit.web.model.ScanSummary
import org.ossreviewtoolkit.web.model.ScannerRun
import org.ossreviewtoolkit.web.model.WebProject

data class State(
    val entities: Entities = Entities(),
    val ui: UI = UI()
) {
    val analyzerRunsForSelectedPackage: List<AnalyzerRun>
        get() {
            val analyzerRunIds = entities.packagesAnalyzerRuns
                .filter { it.packageId == ui.selectedPackage?.id }
                .map { it.analyzerRunId }

            return entities.analyzerRuns.getAll(analyzerRunIds)
        }

    val analyzerRunsForSelectedRepository: List<AnalyzerRun>
        get() {
            val analyzerRunIds = entities.repositoriesAnalyzerRuns
                .filter { it.repositoryId == ui.selectedProjectRepository?.id }
                .map { it.analyzerRunId }

            return entities.analyzerRuns.getAll(analyzerRunIds)
        }

    val repositoriesForSelectedProject: List<ProjectRepository>
        get() {
            val repositoryIds = entities.projectsRepositories
                .filter { it.projectId == ui.selectedProject?.id }
                .map { it.projectRepositoryId }

            return entities.projectRepositories.getAll(repositoryIds)
        }

    val scannerRunsForSelectedPackage: List<ScannerRun>
        get() {
            val scannerRunIds = entities.packagesScannerRuns
                .filter { it.packageId == ui.selectedPackage?.id }
                .map { it.scannerRunId }

            return entities.scannerRuns.getAll(scannerRunIds)
        }
}

data class Entities(
    val packages: Map<String, Package> = emptyMap(),
    val projects: Map<String, WebProject> = emptyMap(),
    val projectRepositories: Map<String, ProjectRepository> = emptyMap(),
    val analyzerRuns: Map<String, AnalyzerRun> = emptyMap(),
    val scannerRuns: Map<String, ScannerRun> = emptyMap(),
    val scanSummary: ScanSummary? = null,

    val packagesAnalyzerRuns: Set<PackagesAnalyzerRuns> = emptySet(),
    val packagesScannerRuns: Set<PackagesScannerRuns> = emptySet(),
    val projectsRepositories: Set<ProjectsRepositories> = emptySet(),
    val repositoriesAnalyzerRuns: Set<RepositoriesAnalyzerRuns> = emptySet()
)

data class PackagesAnalyzerRuns(
    val packageId: String,
    val analyzerRunId: String
)

data class PackagesScannerRuns(
    val packageId: String,
    val scannerRunId: String
)

data class ProjectsRepositories(
    val projectId: String,
    val projectRepositoryId: String
)

data class RepositoriesAnalyzerRuns(
    val repositoryId: String,
    val analyzerRunId: String
)

data class UI(
    val dependencyTree: List<DependencyNode> = emptyList(),
    val filteredPackages: List<PackageTableRow> = emptyList(),
    val filteredProjects: List<WebProject> = emptyList(),
    val packageManagerStats: Map<String, Int> = emptyMap(),
    val packagesFilter: String? = null,
    val projectsFilter: String? = null,
    val scopeStats: Map<String, Int> = emptyMap(),
    val selectedAnalyzerRun: AnalyzerRun? = null,
    val selectedPackage: Package? = null,
    val selectedProject: WebProject? = null,
    val selectedProjectRepository: ProjectRepository? = null,
    val selectedScannerRun: ScannerRun? = null
)

sealed class Action : RAction {
    data class SetAnalyzerRunsForPackage(val pkg: Package, val analyzerRuns: List<AnalyzerRun>) : Action()
    data class SetAnalyzerRunsForProjectRepository(
        val projectRepository: ProjectRepository,
        val analyzerRuns: List<AnalyzerRun>
    ) : Action()

    data class SetPackages(val packages: List<Package>) : Action()
    data class SetPackagesFilter(val packagesFilter: String?) : Action()
    data class SetProjectRepositories(val project: WebProject, val repositories: List<ProjectRepository>) : Action()
    data class SetProjects(val projects: List<WebProject>) : Action()
    data class SetProjectsFilter(val projectsFilter: String?) : Action()
    data class SetScannerRuns(val scannerRuns: List<ScannerRun>) : Action()
    data class SetScannerRunsForPackage(val pkg: Package, val scannerRuns: List<ScannerRun>) : Action()
    data class SetScanSummary(val scanSummary: ScanSummary) : Action()
    data class SetSelectedAnalyzerRun(val analyzerRun: AnalyzerRun?) : Action()
    data class SetSelectedPackage(val pkg: Package?) : Action()
    data class SetSelectedProject(val project: WebProject?) : Action()
    data class SetSelectedProjectRepository(val projectRepository: ProjectRepository?) : Action()
    data class SetSelectedScannerRun(val scannerRun: ScannerRun?) : Action()
}

fun reducer(state: State, action: Action): State = when (action) {
    is Action.SetAnalyzerRunsForPackage -> {
        state.copy(
            entities = state.entities.copy(
                analyzerRuns = action.analyzerRuns.associateBy { it.id },
                packagesAnalyzerRuns = action.analyzerRuns.mapTo(mutableSetOf()) {
                    PackagesAnalyzerRuns(action.pkg.id, it.id)
                },
                repositoriesAnalyzerRuns = emptySet()
            )
        )
    }

    is Action.SetAnalyzerRunsForProjectRepository -> {
        state.copy(
            entities = state.entities.copy(
                analyzerRuns = action.analyzerRuns.associateBy { it.id },
                packagesAnalyzerRuns = emptySet(),
                repositoriesAnalyzerRuns = action.analyzerRuns.mapTo(mutableSetOf()) {
                    RepositoriesAnalyzerRuns(action.projectRepository.id, it.id)
                }
            )
        )
    }

    is Action.SetPackages -> {
        state.copy(
            entities = state.entities.copy(
                packages = action.packages.associateBy { it.id }
            ),
            ui = state.ui.copy(
                filteredPackages = action.packages.filter(state.ui.packagesFilter).map { it.toTableRow() }
            )
        )
    }

    is Action.SetPackagesFilter -> {
        state.copy(
            ui = state.ui.copy(
                packagesFilter = action.packagesFilter,
                filteredPackages = state.entities.packages.values.filter(action.packagesFilter)
                    .map { it.toTableRow() }
            )
        )
    }

    is Action.SetProjectRepositories -> {
        state.copy(
            entities = state.entities.copy(
                projectRepositories = action.repositories.associateBy { it.id },
                projectsRepositories = action.repositories.mapTo(mutableSetOf()) {
                    ProjectsRepositories(action.project.id, it.id)
                }
            )
        )
    }

    is Action.SetProjects -> {
        state.copy(
            entities = state.entities.copy(
                projects = action.projects.associateBy { it.id }
            ),
            ui = state.ui.copy(
                filteredProjects = action.projects.filter(state.ui.projectsFilter)
            )
        )
    }

    is Action.SetProjectsFilter -> {
        state.copy(
            ui = state.ui.copy(
                projectsFilter = action.projectsFilter,
                filteredProjects = state.entities.projects.values.filter(action.projectsFilter)
            )
        )
    }

    is Action.SetScannerRuns -> {
        state.copy(
            entities = state.entities.copy(
                scannerRuns = action.scannerRuns.associateBy { it.id },
                packagesScannerRuns = emptySet()
            )
        )
    }

    is Action.SetScannerRunsForPackage -> {
        state.copy(
            entities = state.entities.copy(
                scannerRuns = action.scannerRuns.associateBy { it.id },
                packagesScannerRuns = action.scannerRuns.mapTo(mutableSetOf()) {
                    PackagesScannerRuns(action.pkg.id, it.id)
                }
            )
        )
    }

    is Action.SetScanSummary -> {
        state.copy(
            entities = state.entities.copy(
                scanSummary = action.scanSummary
            )
        )
    }

    is Action.SetSelectedAnalyzerRun -> {
        state.copy(
            ui = state.ui.copy(
                dependencyTree = action.analyzerRun?.buildDependencyTree().orEmpty(),
                packageManagerStats = action.analyzerRun?.buildPackageManagerStats().orEmpty(),
                scopeStats = action.analyzerRun?.buildScopeStats().orEmpty(),
                selectedAnalyzerRun = action.analyzerRun
            )
        )
    }

    is Action.SetSelectedPackage -> {
        state.copy(
            ui = state.ui.copy(
                selectedPackage = action.pkg
            )
        )
    }

    is Action.SetSelectedProject -> {
        state.copy(
            ui = state.ui.copy(
                selectedProject = action.project
            )
        )
    }

    is Action.SetSelectedProjectRepository -> {
        state.copy(
            ui = state.ui.copy(
                selectedProjectRepository = action.projectRepository
            )
        )
    }

    is Action.SetSelectedScannerRun -> {
        state.copy(
            ui = state.ui.copy(
                selectedScannerRun = action.scannerRun
            )
        )
    }
}

private fun Collection<Package>.filter(filter: String?) =
    filter {
        it.identifier.toCoordinates().contains(filter ?: "") ||
                it.concludedLicense.contains(filter ?: "") ||
                it.declaredLicenses.any { it.contains(filter ?: "") } ||
                it.detectedLicenses.any { it.contains(filter ?: "") }
    }

private fun Collection<WebProject>.filter(filter: String?) = filter { it.name.contains(filter ?: "") }

private fun <K, V> Map<K, V>.getAll(keys: Collection<K>): List<V> = keys.mapNotNull { get(it) }

data class PackageTableRow(
    val id: String,
    val identifier: String,
    val declaredLicenses: String,
    val concludedLicense: String,
    val detectedLicenses: String,
    val lastScanStatus: String,
    val lastScanId: String
)

fun Package.toTableRow() = PackageTableRow(
    id = id,
    identifier = identifier.toCoordinates(),
    declaredLicenses = declaredLicenses.limit(3).joinToString(),
    concludedLicense = concludedLicense,
    detectedLicenses = detectedLicenses.limit(3).joinToString(),
    lastScanStatus = lastScanStatus?.name.orEmpty(),
    lastScanId = lastScanId.orEmpty()
)

fun Collection<String>.limit(limit: Int) = if (size > limit) take(limit) + "..." else this
