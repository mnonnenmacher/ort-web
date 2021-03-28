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

import java.util.UUID

import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

import org.ossreviewtoolkit.web.model.ProjectDao
import org.ossreviewtoolkit.web.model.ProjectRepository
import org.ossreviewtoolkit.web.model.ProjectRepositoryDao
import org.ossreviewtoolkit.web.model.Repositories
import org.ossreviewtoolkit.web.model.Repository
import org.ossreviewtoolkit.web.model.RepositoryDao
import org.ossreviewtoolkit.web.model.WebProject
import org.ossreviewtoolkit.web.toUUID

actual class ProjectService : IProjectService {
    override suspend fun createProject(project: WebProject): WebProject {
        return try {
            // TODO: Validation

            val newProject = transaction {
                ProjectDao.new {
                    name = project.name
                }
            }

            newProject.detach()
        } catch (e: Exception) {
            e.printStackTrace()
            throw ServiceException("Could not create project: ${e.message}")
        }
    }

    override suspend fun readProjects(): List<WebProject> {
        return try {
            // TODO: Validation

            val projects = transaction {
                ProjectDao.all().map { it.detach() }
            }

            projects
        } catch (e: Exception) {
            e.printStackTrace()
            throw ServiceException("Could not read projects: ${e.message}")
        }
    }

    override suspend fun updateProject(project: WebProject): WebProject {
        return try {
            // TODO: Validation

            transaction {
                val existingProject = ProjectDao[project.id.toUUID()]
                existingProject.name = project.name

                existingProject.detach()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw ServiceException("Could not update project: ${e.message}")
        }
    }

    override suspend fun deleteProject(project: WebProject) {
        try {
            // TODO: Validation

            transaction {
                ProjectDao[project.id.toUUID()].delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw ServiceException("Could not delete project: ${e.message}")
        }
    }

    override suspend fun addRepository(projectId: String, repository: Repository, path: String) {
        try {
            // TODO: Validation
            transaction {
                val existingRepository = RepositoryDao.find {
                    Repositories.type eq repository.type.name and
                            (Repositories.url eq repository.url)
                }.limit(1).firstOrNull() ?: RepositoryDao.new(UUID.randomUUID()) {
                    type = repository.type
                    url = repository.url
                }

                val project = ProjectDao.findById(projectId.toUUID())

                if (project != null) {
                    ProjectRepositoryDao.new {
                        this.path = path
                        this.project = project
                        this.repository = existingRepository
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw ServiceException("Could not add repository: ${e.message}")
        }
    }

    override suspend fun readRepositories(projectId: String): List<ProjectRepository> {
        return try {
            transaction {
                ProjectDao.findById(projectId.toUUID())?.let { project ->
                    project.projectRepositories.map { it.detach() }
                } ?: emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw ServiceException("Could not read repositories: ${e.message}")
        }
    }
}
