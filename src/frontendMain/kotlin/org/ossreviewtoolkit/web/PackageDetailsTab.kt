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

import io.kvision.html.div
import io.kvision.html.h1
import io.kvision.html.h3
import io.kvision.panel.SimplePanel
import io.kvision.tabulator.ColumnDefinition
import io.kvision.tabulator.Formatter
import io.kvision.tabulator.Layout
import io.kvision.tabulator.PaginationMode
import io.kvision.tabulator.TabulatorOptions
import io.kvision.tabulator.tabulator
import io.kvision.utils.obj
import io.kvision.utils.pc
import io.kvision.utils.px

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PackageDetailsTab(packageId: String) : SimplePanel() {
    init {
        marginTop = 55.px
        padding = 20.px

        GlobalScope.launch {
            Model.updateSelectedPackage(packageId)
        }

        h1(Model.store) { state ->
            when (state.ui.selectedPackage) {
                null -> +"Package not found"
                else -> +state.ui.selectedPackage.identifier.toCoordinates()
            }
        }

        div(Model.store) { state ->
            padding = 20.px

            state.ui.selectedPackage?.let { pkg ->
                div { +"Authors: ${pkg.authors.joinToString()}" }
                div { +"Concluded license: ${pkg.concludedLicense}" }
                div { +"Declared licenses: ${pkg.declaredLicenses.joinToString()}" }
                div { +"Detected licenses: ${pkg.detectedLicenses.joinToString()}" }
                div { +"Source artifact: ${pkg.sourceArtifact}" }
                div { +"VCS: ${pkg.vcsInfo}" }
            }
        }

        h3 { +"Analyzer Runs:" }

        div { +"The list of all analyzer runs where this package was found." }

        tabulator(
            store = Model.store,
            dataFactory = { it.analyzerRunsForSelectedPackage },
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
//                        formatter = Formatter.LINK,
//                        formatterParams = obj {
//                            // See: http://tabulator.info/docs/4.9/format
//                            urlField = "id"
//                            urlPrefix = "#/projects/$projectId/repositories/$repositoryId/analyzer_runs/"
//                        }
                    )
                )
            )
        ) {
            marginTop = 20.px
            width = 80.pc
        }

        h3 { +"Scans:" }

        tabulator(
            store = Model.store,
            dataFactory = { it.scannerRunsForSelectedPackage },
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
                        title = "Provenance",
                        field = "provenance"
                    ),
                    ColumnDefinition(
                        title = "Scanner",
                        field = "scanner"
                    ),
                    ColumnDefinition(
                        title = "Status",
                        field = "status",
                        headerSort = false,
                        formatter = Formatter.LINK,
                        formatterParams = obj {
                            // See: http://tabulator.info/docs/4.9/format
                            urlField = "id"
                            urlPrefix = "#/scanner_runs/"
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
