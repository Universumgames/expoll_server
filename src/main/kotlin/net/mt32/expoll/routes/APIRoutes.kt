package net.mt32.expoll.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.serialization.encodeToString
import net.mt32.expoll.ExpollMail
import net.mt32.expoll.Mail
import net.mt32.expoll.auth.normalAuth
import net.mt32.expoll.entities.User
import net.mt32.expoll.helper.UnixTimestamp
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
        get("ping"){
            call.respondText("pong", ContentType.Text.Plain)
        }
        route("appInfo"){
            get("ios"){
                call.respond(iosPlatformInfo)
            }
        }
        get("mail"){
            //ExpollMail.UserCreationMail("tom.a@universegame.de", "Tom", "https")
            Mail.sendMailAsync(ExpollMail.UserCreationMail(User.loadFromID("4411a4b1-f62a-11ec-bd56-0242ac190002")!!, "http"))
            Mail.sendMailAsync(ExpollMail.UserDeactivationNotificationMail(User.loadFromID("4411a4b1-f62a-11ec-bd56-0242ac190002")!!, UnixTimestamp.now().addDays(30)))
            Mail.sendMailAsync(ExpollMail.UserDeletionInformationMail(User.loadFromID("4411a4b1-f62a-11ec-bd56-0242ac190002")!!))
            Mail.sendMailAsync(ExpollMail.OTPMail(User.loadFromID("4411a4b1-f62a-11ec-bd56-0242ac190002")!!, "otpTest", "http://localhost:8080"))
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
        userRoutes()
        authenticate(normalAuth) {
            pollRoutes()
            voteRoutes()
            notificationRoutes()
        }
        adminRoute()
    }
}