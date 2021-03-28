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

import io.kvision.chart.ChartOptions
import io.kvision.chart.ChartType
import io.kvision.chart.Configuration
import io.kvision.chart.DataSets
import io.kvision.chart.chart
import io.kvision.core.Col
import io.kvision.core.Color
import io.kvision.html.div
import io.kvision.html.h1
import io.kvision.panel.SimplePanel
import io.kvision.panel.hPanel
import io.kvision.panel.vPanel
import io.kvision.tabulator.ColumnDefinition
import io.kvision.tabulator.Layout
import io.kvision.tabulator.TabulatorOptions
import io.kvision.tabulator.tabulator
import io.kvision.utils.pc
import io.kvision.utils.px

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AnalyzerRunDetailsTab(projectId: String, repositoryId: String, analyzerRunId: String) : SimplePanel() {
    init {
        marginTop = 55.px
        padding = 20.px

        GlobalScope.launch {
            Model.updateSelectedProject(projectId)
            Model.updateSelectedRepository(repositoryId)
            Model.updateSelectedAnalyzerRun(analyzerRunId)
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

        div(Model.store) { state ->
            when (state.ui.selectedAnalyzerRun) {
                null -> +"Analyzer run not found"
                else -> +"${state.ui.selectedAnalyzerRun.revision} - ${state.ui.selectedAnalyzerRun.reference}"
            }
        }

        // TODO: show date

        // expandable exclude configuration
        // - option to safe exclude configuration

        hPanel(spacing = 20) {
            marginTop = 20.px

            // - checkboxes for excluded projects and scopes
            // - automatically update exclude config above
            tabulator(
                store = Model.store,
                dataFactory = { it.ui.dependencyTree },
                options = TabulatorOptions(
                    layout = Layout.FITDATASTRETCH,
                    dataTree = true,
                    dataTreeChildField = "dependencies",
                    columns = listOf(
                        // TODO: Format date.
                        ColumnDefinition(
                            title = "Name",
                            field = "name",
                            //formatter = Formatter.DATETIME
                        ),
                        ColumnDefinition(
                            title = "Type",
                            field = "type"
                        ),
//                    ColumnDefinition(
//                        title = "Excluded",
//                        field = "reference"
//                    ),
                        ColumnDefinition(
                            title = "Number of dependencies",
                            field = "dependencyCount"
                        )
                    )
                )
            ) {
                width = 80.pc
            }

            vPanel(Model.store) {
                chart(
                    configuration = Configuration(
                        type = ChartType.PIE,
                        dataSets = listOf(
                            DataSets(
                                data = Model.state.ui.packageManagerStats.values.toList(),
                                backgroundColor = CHART_COLORS
                            )
                        ),
                        labels = Model.state.ui.packageManagerStats.keys.toList(),
                        options = ChartOptions(

                        )
                    )
                )

                chart(
                    configuration = Configuration(
                        type = ChartType.PIE,
                        dataSets = listOf(
                            DataSets(
                                data = Model.state.ui.scopeStats.values.toList(),
                                backgroundColor = CHART_COLORS
                            )
                        ),
                        labels = Model.state.ui.scopeStats.keys.toList()
                    )
                )
            }
        }
    }
}

private val CHART_COLORS = listOf(
    Color.name(Col.ALICEBLUE),
    Color.name(Col.BISQUE),
    Color.name(Col.CORAL),
    Color.name(Col.DEEPSKYBLUE),
    Color.name(Col.GOLDENROD)
)
