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

import io.kvision.form.FormPanel
import io.kvision.form.formPanel
import io.kvision.form.select.Select
import io.kvision.form.text.Text
import io.kvision.html.ButtonStyle
import io.kvision.html.button
import io.kvision.html.h1
import io.kvision.i18n.tr
import io.kvision.modal.Dialog
import io.kvision.panel.SimplePanel
import io.kvision.remote.ServiceException
import io.kvision.tabulator.ColumnDefinition
import io.kvision.tabulator.Formatter
import io.kvision.tabulator.Layout
import io.kvision.tabulator.PaginationMode
import io.kvision.tabulator.TabulatorOptions
import io.kvision.tabulator.tabulator
import io.kvision.toast.Toast
import io.kvision.utils.obj
import io.kvision.utils.pc
import io.kvision.utils.px

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

import org.ossreviewtoolkit.web.model.Repository
import org.ossreviewtoolkit.web.model.RepositoryType

class ProjectDetailsTab(id: String) : SimplePanel() {
    init {
        marginTop = 55.px
        padding = 20.px

        GlobalScope.launch {
            Model.updateSelectedProject(id)
        }

        h1(Model.store) { state ->
            when (state.ui.selectedProject) {
                null -> +"Project not found"
                else -> +state.ui.selectedProject.name
            }
        }

        button(tr("Add Repository"), style = ButtonStyle.PRIMARY, icon = "fas fa-plus").onClick {
            AddRepositoryDialog().show()
        }

        tabulator(
            store = Model.store,
            dataFactory = { it.repositoriesForSelectedProject },
            options = TabulatorOptions(
                layout = Layout.FITDATASTRETCH,
                // TODO: Sorting crashes if datatype is enum.
//                initialSort = listOf(sorter("type", "asc"), sorter("url", "asc")),
                pagination = PaginationMode.LOCAL,
                paginationSize = 10,
                columns = listOf(
                    ColumnDefinition(
                        title = "Type",
                        field = "repository.type"
                    ),
                    ColumnDefinition(
                        title = "URL",
                        field = "repository.url",
                        formatter = Formatter.LINK,
                        formatterParams = obj {
                            // See: http://tabulator.info/docs/4.9/format
                            urlField = "id"
                            urlPrefix = "#/projects/$id/repositories/"
                        }
                    ),
                    ColumnDefinition(
                        title = "Path",
                        field = "path"
                    )
                )
            )
        ) {
            marginTop = 20.px
            width = 80.pc
        }
    }
}

class AddRepositoryDialog : Dialog<Boolean>(tr("Create New Project")) {
    @Serializable
    data class ProjectRepositoryModel(
        val type: String,
        val url: String,
        val path: String
    )

    private val formPanel: FormPanel<ProjectRepositoryModel>

    // TODO: Add form.
    init {
        formPanel = formPanel {
            add(
                ProjectRepositoryModel::type,
                Select(
                    options = listOf(
                        "Git" to "Git",
                        "Mercurial" to "Mercurial",
                        "Subversion" to "Subversion",
                        "CVS" to "CVS"
                    ),
                    value = "Git",
                    label = "Type"
                ),
                required = true,
                requiredMessage = tr("Type is required")
            ) { select ->
                (select.getValue()?.toLowerCase() ?: "") in RepositoryType.values().map { it.name.toLowerCase() }
            }

            add(
                ProjectRepositoryModel::url,
                Text(label = tr("URL")).apply {
                    placeholder = tr("URL of the repository")
                },
                required = true,
                requiredMessage = tr("URL is required"),
                validatorMessage = { tr("URL must be valid.") }
            ) { text ->
                val value = text.getValue() ?: ""
                value.isNotBlank()
                // TODO: Validate URL.
            }

            add(
                ProjectRepositoryModel::path,
                Text(label = tr("Path")).apply {
                    placeholder = tr("Path inside the repository")
                },
                required = false
            )

            button(tr("Add"), icon = "fas fa-plus", ButtonStyle.SUCCESS).onClick {
                this@AddRepositoryDialog.add()
            }
        }
    }

    private fun add() {
        if (formPanel.validate()) {
            val model = formPanel.getData()
            val repository = Repository(
                id = "",
                type = RepositoryType.from(model.type) ?: RepositoryType.GIT,
                url = model.url
            )
            val path = model.path.trim()
            this@AddRepositoryDialog.hide()
            GlobalScope.launch {
                try {
                    Model.state.ui.selectedProject?.let { project ->
                        Model.addProjectRepository(project, repository, path)
                        Toast.success(tr("Repository added."))
                    }
                } catch (e: ServiceException) {
                    Toast.error(tr("Could not add repository."))
                }
            }
        }
    }
}
