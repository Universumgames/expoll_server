package net.mt32.expoll.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.serialization.encodeToString
import net.mt32.expoll.config
import net.mt32.expoll.serializable.ServerInfo
import net.mt32.expoll.helper.compareVersion
import net.mt32.expoll.helper.defaultJSON
import net.mt32.expoll.helper.getDataFromAny

fun Route.apiRouting(){
    route("/"){
        get("test") {
            call.respondText("Hello World!")
        }
        get("compliance"){
            val clientVersion = getDataFromAny(call, "version")
            if(clientVersion == null){
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }
            call.respond(HttpStatusCode.OK,"${compareVersion(clientVersion, config.minimumRequiredClientVersion)}")
        }
        get("serverInfo"){
            call.respond(ServerInfo.instance)
        }
        get("metaInfo"){
            call.respondText("""
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
                    "serverInfo": ${defaultJSON.encodeToString(ServerInfo.instance)}
                }
            """.trimIndent(), ContentType.Application.Json
            )
        }
        userRoutes()
        pollRoutes()
        voteRoutes()
        adminRoute()
        authRoutes()
        simpleRoutes()
        notificationRoutes()
    }
}