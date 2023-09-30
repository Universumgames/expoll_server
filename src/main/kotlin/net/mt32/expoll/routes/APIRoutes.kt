package net.mt32.expoll.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.serialization.encodeToString
import net.mt32.expoll.auth.normalAuth
import net.mt32.expoll.helper.defaultJSON
import net.mt32.expoll.routes.admin.adminRoute
import net.mt32.expoll.routes.auth.authRoutes
import net.mt32.expoll.serializable.ServerInfo
import net.mt32.expoll.serializable.iosPlatformInfo

fun Route.apiRouting() {
    route("/") {
        get("test") {
            call.respondText("Hello World!")
        }
        clientCompatibilityRoute()
        get("serverInfo") {
            call.respond(ServerInfo.instance)
        }
        route("appInfo"){
            get("ios"){
                call.respond(iosPlatformInfo)
            }
        }
        get("metaInfo") {
            call.respondText(
                """
                {
                    "body": "${call.receiveText()}",
                    "headers": ${defaultJSON.encodeToString(call.request.headers.toMap())},
                    "cookies": ${defaultJSON.encodeToString(call.request.cookies.rawCookies)},
                    "signedCookies": "req.signedCookies",
                    "url": "${call.request.uri}",
                    "path": "${call.request.uri}",
                    "method": "${call.request.httpMethod.value}",
                    "protocol": "${call.request.local.scheme}",
                    "route": "req.route",
                    "params": ${defaultJSON.encodeToString(call.parameters.toMap())},
                    "hostname": "${call.request.local.serverHost}",
                    "ip": "${call.request.local.remoteAddress}",
                    "httpVersion": "${call.request.local.version}",
                    "secure": "req.secure",
                    "subdomains": "req.subdomains",
                    "xhr": "req.xhr",
                    "serverInfo": ${defaultJSON.encodeToString(ServerInfo.instance)},
                    "localport": ${call.request.local.localPort},
                    "serverport": ${call.request.local.serverPort}
                }
            """.trimIndent(), ContentType.Application.Json
            )
        }
        authRoutes()
        simpleRoutes()
        authenticate(normalAuth) {
            pollRoutes()
            voteRoutes()
            notificationRoutes()
            userRoutes()
        }
        adminRoute()
    }
}