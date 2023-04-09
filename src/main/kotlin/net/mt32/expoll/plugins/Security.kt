package net.mt32.expoll.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.auth.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.sessions.*
import kotlinx.coroutines.runBlocking
import net.mt32.expoll.auth.ExpollJWTCookie
import net.mt32.expoll.auth.adminAuth
import net.mt32.expoll.auth.cookieName
import net.mt32.expoll.auth.normalAuth
import net.mt32.expoll.config
import net.mt32.expoll.entities.Session
import net.mt32.expoll.helper.UnixTimestamp
import net.mt32.expoll.helper.getDataFromAny
import kotlin.collections.set


fun Application.configureSecurity() {

    authentication {
        jwt(normalAuth) {
            realm = config.jwt.realm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(config.jwt.secret))
                    .withAudience(config.jwt.audience)
                    .withIssuer(config.jwt.issuer)
                    .build()
            )
            validate { credential ->
                return@validate Session.loadAndVerify(this, credential)
            }
            authHeader { call ->
                var header = call.request.parseAuthorizationHeader()
                if (header == null) {
                    val jwt = runBlocking { return@runBlocking call.getDataFromAny("jwt") }
                    header = parseAuthorizationHeader("Bearer $jwt")
                }
                return@authHeader header
            }
            challenge { defaultScheme, realm ->
                call.sessions.clear(cookieName)
            }
        }
        jwt(adminAuth) {
            realm = config.jwt.realm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(config.jwt.secret))
                    .withAudience(config.jwt.audience)
                    .withIssuer(config.jwt.issuer)
                    .build()
            )
            validate { credential ->
                return@validate Session.loadAndVerify(this, credential, withAdmin = true)
            }
            authHeader { call ->
                var header = call.request.parseAuthorizationHeader()
                if (header == null) {
                    val jwt = runBlocking { return@runBlocking call.getDataFromAny("jwt") }
                    header = parseAuthorizationHeader("Bearer $jwt")
                }
                return@authHeader header
            }
        }
    }

    install(Sessions) {
        cookie<ExpollJWTCookie>(cookieName) {
            cookie.extensions["SameSite"] = if (!config.developmentMode) "strict" else "lax"
            if (!config.developmentMode) cookie.extensions["Domain"] = config.cookieDomain
            //if (!config.developmentMode) cookie.secure = true
            serializer = ExpollJWTCookie.Companion
            cookie.maxAgeInSeconds = UnixTimestamp.zero().addDays(120).secondsSince1970 // 120 days ?
        }
    }
}
