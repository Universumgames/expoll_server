package net.mt32.expoll.routes.auth

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import net.mt32.expoll.auth.ExpollJWTCookie
import net.mt32.expoll.auth.JWTSessionPrincipal
import net.mt32.expoll.auth.cookieName
import net.mt32.expoll.auth.normalAuth
import net.mt32.expoll.entities.OTP
import net.mt32.expoll.entities.User
import net.mt32.expoll.helper.DeepLinkBuilder
import net.mt32.expoll.helper.ReturnCode
import net.mt32.expoll.serializable.request.SimpleLoginRequest

fun Route.simpleAuthRoutes() {
    route("simple") {
        post {
            simpleLoginRoute(call)
        }
        authenticate(normalAuth) {
            get("app") {
                loginApp(call)
            }
        }
    }
}

suspend fun simpleLoginRoute(call: ApplicationCall) {
    val simpleLoginRequest: SimpleLoginRequest = call.receive()
    val otpString = simpleLoginRequest.otp
    val mail = simpleLoginRequest.mail

    if (otpString.isNullOrEmpty() && mail.isNullOrEmpty()) {
        call.respond(ReturnCode.MISSING_PARAMS)
        return
    }

    if (!mail.isNullOrEmpty()) {
        val forApp = simpleLoginRequest.forApp ?: false
        val user = User.byMail(mail)
        if (user == null || !user.loginAble) {
            call.respond(ReturnCode.BAD_REQUEST)
            return
        }
        user.sendOTPMail(call, forApp)
        call.respond(ReturnCode.OK)
        return
    }

    if (!otpString.isNullOrEmpty()) {
        val cleanedOTP = otpString
            .replace(" ", "")
            .replace("\t", "")
        val otp = OTP.fromOTP(cleanedOTP)
        if (otp == null) {
            call.respond(ReturnCode.UNAUTHORIZED)
            return
        }
        val session = otp.createSessionAndDeleteSelf(
            call.request.headers["User-Agent"] ?: "unknown",
            simpleLoginRequest.version,
            simpleLoginRequest.platform
        )
        val jwt = session.getJWT()
        call.sessions.clear(cookieName)
        call.sessions.set(ExpollJWTCookie(jwt))
        call.response.headers.append("forApp", otp.forApp.toString())
        call.respondText(jwt)
        return
    }
    call.respond(ReturnCode.INTERNAL_SERVER_ERROR)
}

private suspend fun loginApp(call: ApplicationCall) {
    val principal = call.principal<JWTSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.UNAUTHORIZED)
        return
    }
    val otp = principal.user.createOTP(true)
    call.respondRedirect(DeepLinkBuilder.buildLoginLink(call, otp, false))
}