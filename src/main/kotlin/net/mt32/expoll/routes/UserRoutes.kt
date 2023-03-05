package net.mt32.expoll.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRoutes(){
    authenticate {
        route("/user") {
            get{
                call.respondText { "test" }
            }
        }
    }
}