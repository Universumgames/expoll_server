package net.mt32.expoll.routes.admin

import io.ktor.server.auth.*
import io.ktor.server.routing.*
import net.mt32.expoll.auth.adminAuth

fun Route.adminRoute() {
    route("/admin") {
        authenticate(adminAuth) {
            adminUserRoutes()
            adminPollRoutes()
            adminRegexRoutes()
        }
        adminImpersonateRoutes()
    }
}

