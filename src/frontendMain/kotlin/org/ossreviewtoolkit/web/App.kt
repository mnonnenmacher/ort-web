package org.ossreviewtoolkit.web

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import io.kvision.Application
import io.kvision.html.Span
import io.kvision.html.div
import io.kvision.html.span
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
import io.kvision.utils.px

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
                    navLink(tr("Packages"), icon = "fas fa-archive")
                    navLink(tr("Repositories"), icon = "fas fa-database")
                    navLink(tr("Source Artifacts"), icon = "fas fa-file-archive")
                }
            }

            routing
                .on("/projects", { add(ProjectsTab()) })
                .resolve() as Unit
        }
        GlobalScope.launch {
            val pingResult = Model.ping("Hello world from client!")
            root.add(Span(pingResult))
        }
    }
}

fun main() {
    startApplication(::App, module.hot)
}
