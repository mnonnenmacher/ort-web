package org.ossreviewtoolkit.web

import io.kvision.html.span
import io.kvision.i18n.tr
import io.kvision.panel.SimplePanel
import io.kvision.utils.px

class ProjectsTab : SimplePanel() {
    init {
        marginTop = 55.px

        span {
            +tr("Projects")
        }
    }
}
