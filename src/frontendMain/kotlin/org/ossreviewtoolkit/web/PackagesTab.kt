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
import io.kvision.form.text.TextInputType
import io.kvision.form.text.text
import io.kvision.i18n.tr
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

class PackagesTab : SimplePanel() {
    init {
        marginTop = 55.px
        padding = 20.px

        text {
            marginTop = 20.px
            type = TextInputType.SEARCH
            placeholder = tr("Search...")
            autofocus = true
            onEvent {
                input = {
                    Model.store.dispatch(Action.SetPackagesFilter(self.value))
                }
            }
        }

        tabulator(
            store = Model.store,
            dataFactory = { it.ui.filteredPackages },
            options = TabulatorOptions(
                layout = Layout.FITDATASTRETCH,
                initialSort = listOf(sorter("identifier", "asc")),
                pagination = PaginationMode.LOCAL,
                paginationSize = 10,
                columns = listOf(
                    ColumnDefinition(
                        title = "Identifier",
                        field = "identifier",
                        formatter = Formatter.LINK,
                        formatterParams = obj {
                            // See: http://tabulator.info/docs/4.9/format
                            urlField = "id"
                            urlPrefix = "#/packages/"
                        }
                    ),
                    ColumnDefinition(
                        title = "Concluded License",
                        field = "concludedLicense"
                    ),
                    ColumnDefinition(
                        title = "Declared Licenses",
                        field = "declaredLicenses"
                    ),
                    ColumnDefinition(
                        title = "Detected Licenses",
                        field = "detectedLicenses"
                    ),
                    ColumnDefinition(
                        title = "Last Scan",
                        field = "lastScanStatus",
                        formatter = Formatter.LINK,
                        formatterParams = obj {
                            // See: http://tabulator.info/docs/4.9/format
                            urlField = "lastScanId"
                            urlPrefix = "#/scanner_run/"
                        }
                    )
                )
            )
        ) {
            marginTop = 20.px
            width = 80.pc
        }

        GlobalScope.launch {
            console.log("Loading packages.")
            if (Model.state.entities.packages.isEmpty()) {
                Model.updatePackages()
            }
        }
    }
}
