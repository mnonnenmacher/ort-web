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
import io.kvision.tabulator.Layout
import io.kvision.tabulator.PaginationMode
import io.kvision.tabulator.TabulatorOptions
import io.kvision.tabulator.tabulator
import io.kvision.utils.pc
import io.kvision.utils.px

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ScannerRunDetailsTab(scannerRunId: String) : SimplePanel() {
    init {
        marginTop = 55.px
        padding = 20.px

        GlobalScope.launch {
            Model.updateSelectedScannerRun(scannerRunId)
        }

        h1(Model.store) { state ->
            when (state.ui.selectedScannerRun) {
                null -> +"Scanner run not found"
                else -> +"Scan ${state.ui.selectedScannerRun.id}"
            }
        }

        div(Model.store) { state ->
            state.ui.selectedScannerRun?.let { scannerRun ->
                div { +"Provenance: ${scannerRun.provenance}" }
                div { +"Scanner: ${scannerRun.scanner}" }
                div { +"Status: ${scannerRun.status}" }
            }

            state.entities.scanSummary?.let { scanSummary ->
                div { +"File count: ${scanSummary.fileCount}" }
                div { +"Package verification code: ${scanSummary.packageVerificationCode}" }
            }
        }

        h3 { +"License Findings"}

        tabulator(
            store = Model.store,
            dataFactory = { it.entities.scanSummary?.licenseFindings.orEmpty().toList() },
            options = TabulatorOptions(
                layout = Layout.FITDATASTRETCH,
                //initialSort = listOf(sorter("timestamp", "asc")),
                pagination = PaginationMode.LOCAL,
                paginationSize = 10,
                columns = listOf(
                    // TODO: Format date.
                    ColumnDefinition(
                        title = "License",
                        field = "license",
                        //formatter = Formatter.DATETIME
                    ),
                    ColumnDefinition(
                        title = "Path",
                        field = "location.path"
                    ),
                    ColumnDefinition(
                        title = "Start Line",
                        field = "location.startLine"
                    ),
                    ColumnDefinition(
                        title = "End Line",
                        field = "location.endLine"
                    )
                )
            )
        ) {
            marginTop = 20.px
            width = 80.pc
        }

        h3 { +"Copyright Findings"}

        tabulator(
            store = Model.store,
            dataFactory = { it.entities.scanSummary?.copyrightFindings.orEmpty().toList() },
            options = TabulatorOptions(
                layout = Layout.FITDATASTRETCH,
                //initialSort = listOf(sorter("timestamp", "asc")),
                pagination = PaginationMode.LOCAL,
                paginationSize = 10,
                columns = listOf(
                    // TODO: Format date.
                    ColumnDefinition(
                        title = "Statement",
                        field = "statement",
                        //formatter = Formatter.DATETIME
                    ),
                    ColumnDefinition(
                        title = "Path",
                        field = "location.path"
                    ),
                    ColumnDefinition(
                        title = "Start Line",
                        field = "location.startLine"
                    ),
                    ColumnDefinition(
                        title = "End Line",
                        field = "location.endLine"
                    )
                )
            )
        ) {
            marginTop = 20.px
            width = 80.pc
        }

        h3 { +"Issues"}

        tabulator(
            store = Model.store,
            dataFactory = { it.entities.scanSummary?.issues.orEmpty().toList() },
            options = TabulatorOptions(
                layout = Layout.FITDATASTRETCH,
                //initialSort = listOf(sorter("timestamp", "asc")),
                pagination = PaginationMode.LOCAL,
                paginationSize = 10,
                columns = listOf(
                    // TODO: Format date.
                    ColumnDefinition(
                        title = "Source",
                        field = "source",
                        //formatter = Formatter.DATETIME
                    ),
                    ColumnDefinition(
                        title = "Severity",
                        field = "severity"
                    ),
                    ColumnDefinition(
                        title = "Message",
                        field = "message"
                    )
                )
            )
        ) {
            marginTop = 20.px
            width = 80.pc
        }
    }
}
