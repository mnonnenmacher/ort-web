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
import io.kvision.form.text.Text
import io.kvision.html.ButtonStyle
import io.kvision.html.button
import io.kvision.html.div
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

class RepositoryDetailsTab(projectId: String, repositoryId: String) : SimplePanel() {
    init {
        marginTop = 55.px
        padding = 20.px

        GlobalScope.launch {
            Model.updateSelectedProject(projectId)
            Model.updateSelectedRepository(repositoryId)
        }

        h1(Model.store) { state ->
            when (state.ui.selectedProject) {
                null -> +"Project not found"
                else -> +state.ui.selectedProject.name
            }
        }

        div(Model.store) { state ->
            when (state.ui.selectedProjectRepository) {
                null -> +"Repository not found"
                else -> +"${state.ui.selectedProjectRepository.repository.type} - ${state.ui.selectedProjectRepository.repository.url} - ${state.ui.selectedProjectRepository.path}"
            }
        }

        button(tr("Start Analyzer"), style = ButtonStyle.PRIMARY, icon = "fas fa-plus").onClick {
            StartAnalyzerDialog().show()
        }

        tabulator(
            store = Model.store,
            dataFactory = { it.analyzerRunsForSelectedRepository },
            options = TabulatorOptions(
                layout = Layout.FITDATASTRETCH,
                //initialSort = listOf(sorter("timestamp", "asc")),
                pagination = PaginationMode.LOCAL,
                paginationSize = 10,
                columns = listOf(
                    // TODO: Format date.
                    ColumnDefinition(
                        title = "Date",
                        field = "timestamp",
                        //formatter = Formatter.DATETIME
                    ),
                    ColumnDefinition(
                        title = "Revision",
                        field = "revision"
                    ),
                    ColumnDefinition(
                        title = "Reference",
                        field = "reference"
                    ),
                    ColumnDefinition(
                        title = "Status",
                        field = "status",
                        headerSort = false,
                        formatter = Formatter.LINK,
                        formatterParams = obj {
                            // See: http://tabulator.info/docs/4.9/format
                            urlField = "id"
                            urlPrefix = "#/projects/$projectId/repositories/$repositoryId/analyzer_runs/"
                        }
                    )
                )
            )
        ) {
            marginTop = 20.px
            width = 80.pc
        }
    }
}

class StartAnalyzerDialog : Dialog<Boolean>(tr("Start Analyzer")) {
    @Serializable
    data class AnalyzerRunModel(
        val revision: String
    )

    private val formPanel: FormPanel<AnalyzerRunModel>

    // TODO: Add form.
    init {
        formPanel = formPanel {
            // TODO: Add select for branches and tags.

            add(
                AnalyzerRunModel::revision,
                Text(label = tr("Revision")).apply {
                    placeholder = tr("Revision of the repository")
                },
                required = true,
                requiredMessage = tr("Revision is required"),
                validatorMessage = { tr("Revision must be valid.") }
            ) { text ->
                val value = text.getValue() ?: ""
                value.isNotBlank()
            }

            button(tr("Start"), icon = "fas fa-play", ButtonStyle.SUCCESS).onClick {
                this@StartAnalyzerDialog.start()
            }
        }
    }

    private fun start() {
        if (formPanel.validate()) {
            val model = formPanel.getData()
            this@StartAnalyzerDialog.hide()
            GlobalScope.launch {
                try {
                    Model.state.ui.selectedProjectRepository?.let { repository ->
                        Model.startAnalyzer(repository.id, model.revision)
                        Toast.success(tr("Analyzer started."))
                    }
                } catch (e: ServiceException) {
                    Toast.error(tr("Could not start analyzer."))
                }
            }
        }
    }
}
