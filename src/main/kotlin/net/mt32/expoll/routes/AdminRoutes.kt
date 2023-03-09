package net.mt32.expoll.routes

import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.adminRoute() {
    authenticate {
        route("admin") {
// TODO add admin routes
        }
    }
}