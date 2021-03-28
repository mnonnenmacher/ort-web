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

import io.kvision.Application
import io.kvision.core.Component
import io.kvision.i18n.DefaultI18nManager
import io.kvision.i18n.I18n
import io.kvision.i18n.tr
import io.kvision.module
import io.kvision.navbar.NavbarType
import io.kvision.navbar.nav
import io.kvision.navbar.navLink
import io.kvision.navbar.navbar
import io.kvision.panel.root
import io.kvision.require
import io.kvision.routing.Routing
import io.kvision.routing.Strategy
import io.kvision.routing.routing
import io.kvision.startApplication

import kotlin.js.RegExp

class App : Application() {

    override fun start(state: Map<String, Any>) {
        I18n.manager =
            DefaultI18nManager(
                mapOf(
                    "en" to require("i18n/messages-en.json")
                )
            )

        Routing.init(root = "/", useHash = true, strategy = Strategy.ALL)

        val root = root("kvapp") {
            navbar(label = "OSS Review Toolkit", type = NavbarType.FIXEDTOP) {
                nav {
                    navLink(tr("Projects"), icon = "fas fa-project-diagram", url = "/#/projects")
                    navLink(tr("Packages"), icon = "fas fa-archive", url = "/#/packages")
                    navLink(tr("Repositories"), icon = "fas fa-database")
                    navLink(tr("Source Artifacts"), icon = "fas fa-file-archive")
                }
            }

            var content: Component? = null
            fun updateContent(component: Component) {
                content?.let { remove(it) }
                content = component
                add(component)
            }

            routing
                .on("/packages", { updateContent(PackagesTab()) })
                .on(RegExp("^packages/([0-9a-f\\-]+)"), { match ->
                    val id = match.data[0] as String
                    updateContent(PackageDetailsTab(id))
                })
                .on("/projects", { updateContent(ProjectsTab()) })
                .on(RegExp("^projects/([0-9a-f\\-]+)"), { match ->
                    val id = match.data[0] as String
                    updateContent(ProjectDetailsTab(id))
                })
                .on(RegExp("^projects/([0-9a-f\\-]+)/repositories/([0-9a-f\\-]+)"), { match ->
                    val projectId = match.data[0] as String
                    val repositoryId = match.data[1] as String
                    updateContent(RepositoryDetailsTab(projectId, repositoryId))
                })
                .on(RegExp("^projects/([0-9a-f\\-]+)/repositories/([0-9a-f\\-]+)/analyzer_runs/([0-9a-f\\-]+)"), {match->
                    val projectId = match.data[0] as String
                    val repositoryId = match.data[1] as String
                    val analyzerRunId = match.data[2] as String
                    updateContent(AnalyzerRunDetailsTab(projectId, repositoryId, analyzerRunId))
                })
                .on(RegExp("^scanner_runs/([0-9a-f\\-]+)"), {match->
                    val scannerRunId = match.data[0] as String
                    updateContent(ScannerRunDetailsTab(scannerRunId))
                })
                .resolve() as Unit
        }
    }
}

fun main() {
    startApplication(::App, module.hot)
}
