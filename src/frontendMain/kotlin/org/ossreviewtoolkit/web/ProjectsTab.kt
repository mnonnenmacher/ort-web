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

import io.kvision.core.onEvent
import io.kvision.form.FormPanel
import io.kvision.form.formPanel
import io.kvision.form.text.Text
import io.kvision.form.text.TextInputType
import io.kvision.form.text.text
import io.kvision.html.ButtonStyle
import io.kvision.html.button
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

import org.ossreviewtoolkit.web.model.WebProject

class ProjectsTab : SimplePanel() {
    init {
        marginTop = 55.px
        padding = 20.px

        button(tr("Create New Project"), style = ButtonStyle.PRIMARY, icon = "fas fa-plus").onClick {
            CreateProjectDialog().show()
        }

        text {
            marginTop = 20.px
            type = TextInputType.SEARCH
            placeholder = tr("Search...")
            autofocus = true
            onEvent {
                input = {
                    Model.store.dispatch(Action.SetProjectsFilter(self.value))
                }
            }
        }

        tabulator(
            store = Model.store,
            dataFactory = { it.ui.filteredProjects },
            options = TabulatorOptions(
                layout = Layout.FITDATASTRETCH,
                //initialSort = listOf(sorter("name", "asc")),
                pagination = PaginationMode.LOCAL,
                paginationSize = 10,
                columns = listOf(
                    ColumnDefinition(
                        title = "Name",
                        field = "name",
                        formatter = Formatter.LINK,
                        formatterParams = obj {
                            // See: http://tabulator.info/docs/4.9/format
                            urlField = "id"
                            urlPrefix = "#/projects/"
                        }
                    )
                )
            )
        ) {
            marginTop = 20.px
            width = 80.pc
        }

        GlobalScope.launch {
            console.log("Loading projects.")
            if (Model.state.entities.projects.isEmpty()) {
                Model.updateProjects()
            }
        }
    }
}

class CreateProjectDialog : Dialog<Boolean>(tr("Create New Project")) {
    @Serializable
    data class ProjectModel(val name: String)

    private val formPanel: FormPanel<ProjectModel>

    // TODO: Add form.
    init {
        formPanel = formPanel {
            add(
                ProjectModel::name,
                Text(label = tr("Name")).apply {
                    placeholder = tr("Name of the project")
                },
                required = true,
                requiredMessage = tr("Name is required"),
                validatorMessage = { tr("Name must match ...") }
            ) {
                val value = it.getValue() ?: ""
                value.isNotBlank()
                // TODO: Validate characters.
            }

            button(tr("Create"), icon = "fas fa-plus", ButtonStyle.SUCCESS).onClick {
                this@CreateProjectDialog.create()
            }
        }
    }

    private fun create() {
        if (formPanel.validate()) {
            val model = formPanel.getData()
            val project = WebProject("", model.name)
            this@CreateProjectDialog.hide()
            GlobalScope.launch {
                try {
                    Model.createProject(project)
                    Toast.success(tr("Project created."))
                } catch (e: ServiceException) {
                    Toast.error(tr("Could not create project."))
                }
            }
        }
    }
}
