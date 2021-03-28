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

import io.kvision.redux.createReduxStore
import io.kvision.remote.ServiceException

import org.ossreviewtoolkit.web.model.AnalyzerRun
import org.ossreviewtoolkit.web.model.PackageReference
import org.ossreviewtoolkit.web.model.Project
import org.ossreviewtoolkit.web.model.Repository
import org.ossreviewtoolkit.web.model.Scope
import org.ossreviewtoolkit.web.model.WebProject
import org.ossreviewtoolkit.web.service.AnalyzerService
import org.ossreviewtoolkit.web.service.PackageService
import org.ossreviewtoolkit.web.service.ProjectService
import org.ossreviewtoolkit.web.service.ScannerService

object Model {

    private val analyzerService = AnalyzerService()
    private val packageService = PackageService()
    private val projectService = ProjectService()
    private val scannerService = ScannerService()

    val store = createReduxStore(::reducer, State())
    val state: State get() = store.getState()

    suspend fun createProject(project: WebProject) {
        projectService.createProject(project)
        updateProjects()
    }

    suspend fun updatePackages() {
        try {
            val packages = packageService.readPackages()
            console.log("Read ${packages.size} packages.")
            store.dispatch(Action.SetPackages(packages))
        } catch (e: ServiceException) {
            console.log("Could not read packages: ${e.message}")
        }
    }

    suspend fun updateProjects() {
        try {
            val projects = projectService.readProjects()
            console.log("Read ${projects.size} projects.")
            store.dispatch(Action.SetProjects(projects))
        } catch (e: ServiceException) {
            console.log("Could not read projects: ${e.message}")
        }
    }

    suspend fun updateSelectedPackage(id: String) {
        if (state.entities.packages.isEmpty()) updatePackages()

        val pkg = state.entities.packages[id]
        store.dispatch(Action.SetSelectedPackage(pkg))

        updateAnalyzerRunsForSelectedPackage()
        updateScannerRunsForSelectedPackage()
    }

    suspend fun updateSelectedProject(id: String) {
        if (state.entities.projects.isEmpty()) updateProjects()

        val project = state.entities.projects[id]
        store.dispatch(Action.SetSelectedProject(project))

        updateRepositoriesForSelectedProject()
    }

    suspend fun addProjectRepository(project: WebProject, repository: Repository, path: String) {
        projectService.addRepository(project.id, repository, path)
        updateRepositoriesForSelectedProject()
    }

    suspend fun updateRepositoriesForSelectedProject() {
        try {
            state.ui.selectedProject?.let { project ->
                val repositories = projectService.readRepositories(project.id)
                console.log("Read ${repositories.size} project repositories.")
                store.dispatch(Action.SetProjectRepositories(project, repositories))
            }
        } catch (e: ServiceException) {
            console.log("Could not read project repositories: ${e.message}")
        }
    }

    suspend fun updateSelectedRepository(id: String) {
        try {
            val repository = state.entities.projectRepositories[id]

            if (repository != null) {
                store.dispatch(Action.SetSelectedProjectRepository(repository))
            } else {
                store.dispatch(Action.SetSelectedProjectRepository(null))
            }

            updateAnalyzerRunsForSelectedRepository()
            // TODO: If repository with [id] not in list update repositories.
            // TODO: Fetch list of VCS references for repository.
            // TODO: Fetch list of scan results for repository.
        } catch (e: ServiceException) {
            console.log("Could not update selected repository: ${e.message}")
        }
    }

    suspend fun startAnalyzer(repositoryId: String, revision: String) {
        analyzerService.startAnalyzer(repositoryId, revision)
        updateAnalyzerRunsForSelectedRepository()
    }

    suspend fun updateAnalyzerRunsForSelectedPackage() {
        try {
            state.ui.selectedPackage?.let { pkg ->
                val analyzerRuns = analyzerService.readAnalyzerRunsForPackage(pkg.id)
                console.log("Read ${analyzerRuns.size} analyzer runs.")
                store.dispatch(Action.SetAnalyzerRunsForPackage(pkg, analyzerRuns))
            }
        } catch (e: ServiceException) {
            console.log("Could not read analyzer runs: ${e.message}")
        }
    }

    suspend fun updateScannerRuns() {
        try {
            val scannerRuns = scannerService.readScannerRuns()
            console.log("Read ${scannerRuns.size} scanner runs.")
            store.dispatch(Action.SetScannerRuns(scannerRuns))
        } catch (e: ServiceException) {
            console.log("Could not read scanner runs: ${e.message}")
        }

    }

    suspend fun updateScannerRunsForSelectedPackage() {
        try {
            state.ui.selectedPackage?.let { pkg ->
                val scannerRuns = scannerService.readScannerRunsForPackage(pkg.id)
                console.log("Read ${scannerRuns.size} scanner runs.")
                store.dispatch(Action.SetScannerRunsForPackage(pkg, scannerRuns))
            }
        } catch (e: ServiceException) {
            console.log("Could not read scanner runs: ${e.message}")
        }
    }

    suspend fun updateAnalyzerRunsForSelectedRepository() {
        try {
            state.ui.selectedProjectRepository?.let { repository ->
                val analyzerRuns = analyzerService.readAnalyzerRunsForProjectRepository(repository.id)
                console.log("Read ${analyzerRuns.size} analyzer runs.")
                store.dispatch(Action.SetAnalyzerRunsForProjectRepository(repository, analyzerRuns))
            }
        } catch (e: ServiceException) {
            console.log("Could not read analyzer runs: ${e.message}")
        }
    }

    suspend fun updateSelectedAnalyzerRun(id: String) {
        try {
            val analyzerRun = state.entities.analyzerRuns[id]

            store.dispatch(Action.SetSelectedAnalyzerRun(analyzerRun))

            // TODO: update dependency tree data
        } catch (e: ServiceException) {
            console.log("Could not update selected analyzer run: ${e.message}")
        }
    }

    suspend fun updateSelectedScannerRun(id: String) {
        try {
            if (state.entities.scannerRuns.isEmpty()) updateScannerRuns()

            val scannerRun = state.entities.scannerRuns[id]

            store.dispatch(Action.SetSelectedScannerRun(scannerRun))

            updateScanSummaryForSelectedScannerRun()
            // TODO: update dependency tree data
        } catch (e: ServiceException) {
            console.log("Could not update selected scanner run: ${e.message}")
        }
    }

    suspend fun updateScanSummaryForSelectedScannerRun() {
        try {
            state.ui.selectedScannerRun?.let { scannerRun ->
                val scanSummary = scannerService.readScanSummary(scannerRun.id)
                console.log("Read scan summary for ${scannerRun.id}.")
                store.dispatch(Action.SetScanSummary(scanSummary))
            }
        } catch (e: ServiceException) {
            console.log("Could not read scanner runs: ${e.message}")
        }
    }
}

enum class DependencyTreeElementType {
    PROJECT, SCOPE, DEPENDENCY
}

// TODO: Add issues field below.
data class DependencyNode(
    val name: String,
    val type: DependencyTreeElementType,
    val isExcluded: Boolean,
    val dependencyCount: Int,
    val dependencies: Array<DependencyNode>?
)

fun AnalyzerRun.buildDependencyTree(): List<DependencyNode> = projects.map { it.toDependencyNode() }

fun Project.toDependencyNode(): DependencyNode = DependencyNode(
    name = id.toCoordinates(),
    type = DependencyTreeElementType.PROJECT,
    isExcluded = false,
    dependencyCount = countDependencies(),
    dependencies = if (scopes.isNotEmpty()) scopes.map { it.toDependencyNode() }.toTypedArray() else null
)

fun Scope.toDependencyNode(): DependencyNode = DependencyNode(
    name = name,
    type = DependencyTreeElementType.SCOPE,
    isExcluded = false,
    dependencyCount = countDependencies(),
    dependencies = if (dependencies.isNotEmpty()) dependencies.map { it.toDependencyNode() }.toTypedArray() else null
)

fun PackageReference.toDependencyNode(): DependencyNode = DependencyNode(
    name = id.toCoordinates(),
    type = DependencyTreeElementType.DEPENDENCY,
    isExcluded = false,
    dependencyCount = countDependencies(),
    dependencies = if (dependencies.isNotEmpty()) dependencies.map { it.toDependencyNode() }.toTypedArray() else null
)

fun Project.countDependencies(): Int = collectDependencies().size

fun Scope.countDependencies(): Int = collectDependencies().size

fun PackageReference.countDependencies(): Int = collectDependencies().size

fun Project.collectDependencies(): Set<String> = scopes.flatMapTo(mutableSetOf()) { it.collectDependencies() }

fun Scope.collectDependencies(): Set<String> =
    dependencies.mapTo(mutableSetOf()) { it.id.toCoordinates() } +
            dependencies.flatMapTo(mutableSetOf()) { it.collectDependencies() }

fun PackageReference.collectDependencies(): Set<String> =
    dependencies.mapTo(mutableSetOf()) { it.id.toCoordinates() } +
            dependencies.flatMapTo(mutableSetOf()) { it.collectDependencies() }

fun AnalyzerRun.buildPackageManagerStats(): Map<String, Int> =
    projects.groupBy { it.id.type }.mapValues { it.value.size }

fun AnalyzerRun.buildScopeStats(): Map<String, Int> =
    projects.flatMap { it.scopes }.groupBy { it.name }
        .mapValues { it.value.flatMapTo(mutableSetOf()) { it.collectDependencies() }.size }
