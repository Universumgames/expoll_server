package net.mt32.expoll.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.auth.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.coroutines.runBlocking
import net.mt32.expoll.auth.*
import net.mt32.expoll.config
import net.mt32.expoll.entities.Session
import net.mt32.expoll.helper.getDataFromAny
import kotlin.collections.set


fun Application.configureSecurity() {

    authentication {
        checkLoggedIn(normalAuth) {

        }
        checkLoggedIn(adminAuth) {
            checkAdmin = true
        }
        jwt("jwtAuth") {
            realm = config.jwt.realm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(config.jwt.secret))
                    .withAudience(config.jwt.audience)
                    .withIssuer(config.jwt.issuer)
                    .build()
            )
            validate { credential ->
                return@validate Session.loadAndVerify(credential)
            }
            authHeader { call->
                var header = call.request.parseAuthorizationHeader()
                if(header == null){
                    val jwt = runBlocking { return@runBlocking call.getDataFromAny("jwt") }
                    header = parseAuthorizationHeader("Bearer $jwt")
                }
                return@authHeader header
            }
        }
    }
    data class MySession(val count: Int = 0)
    install(Sessions) {
        cookie<ExpollCookie>(oldCookieName) {
            cookie.extensions["SameSite"] = "lax"
            serializer = ExpollCookie.Companion
            cookie.maxAgeInSeconds = 60 * 60 * 24 * 120 // 120 days ?
        }
        cookie<ExpollJWTCookie>(cookieName){
            cookie.extensions["SameSite"] = "lax"
            serializer = ExpollJWTCookie.Companion
            cookie.maxAgeInSeconds = 60 * 60 * 24 * 120 // 120 days ?
        }
        cookie<MySession>("MY_SESSION") {
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
