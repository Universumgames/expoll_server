package net.mt32.expoll.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import net.mt32.expoll.BasicSessionPrincipal
import net.mt32.expoll.ExpollCookie

fun Route.userRoutes(){
    authenticate {
        route("/user") {
            get{
                val session = call.sessions.get<ExpollCookie>()
                val prince = call.principal<BasicSessionPrincipal>()
                print(prince)
                call.respondText { session?.loginKey ?: "not found"}
            }
        }
    }
}