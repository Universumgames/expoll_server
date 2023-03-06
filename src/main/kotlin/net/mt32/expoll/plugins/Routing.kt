package net.mt32.expoll.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.mt32.expoll.routes.apiRouting
import net.mt32.expoll.routes.userRoutes

fun Application.configureRouting() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause" , status = HttpStatusCode.InternalServerError)
        }
    }
    routing {
        apiRouting()
        userRoutes()
        get("/") {
            call.respondText("Hello World!")
        }

    }
}
