package net.mt32.expoll.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import net.mt32.expoll.ExpollCookie
import net.mt32.expoll.checkLoggedIn
import net.mt32.expoll.cookieName
import kotlin.collections.set


fun Application.configureSecurity() {

    authentication {
        checkLoggedIn {

        }
    }
    data class MySession(val count: Int = 0)
    install(Sessions) {
        cookie<ExpollCookie>(cookieName) {
            cookie.extensions["SameSite"] = "lax"
        }
        cookie<MySession>("MY_SESSION"){
            cookie.extensions["SameSite"] = "lax"
        }
    }
    routing {
        get("/session/increment") {
            val session = call.sessions.get<MySession>() ?: MySession()
            call.sessions.set(session.copy(count = session.count + 1))
            call.respondText("Counter is ${session.count}. Refresh to increment.")
        }
    }
}
