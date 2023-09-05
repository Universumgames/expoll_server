package net.mt32.expoll.routes.auth

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import net.mt32.expoll.Mail
import net.mt32.expoll.auth.ExpollJWTCookie
import net.mt32.expoll.auth.JWTSessionPrincipal
import net.mt32.expoll.auth.cookieName
import net.mt32.expoll.auth.normalAuth
import net.mt32.expoll.entities.OTP
import net.mt32.expoll.entities.User
import net.mt32.expoll.helper.DeepLinkBuilder
import net.mt32.expoll.helper.ReturnCode
import net.mt32.expoll.helper.URLBuilder
import net.mt32.expoll.helper.getDataFromAny

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
    val otpString = call.getDataFromAny("otp")
    val mail = call.getDataFromAny("mail")

    if (otpString.isNullOrEmpty() && mail.isNullOrEmpty()) {
        call.respond(ReturnCode.MISSING_PARAMS)
        return
    }

    if (!mail.isNullOrEmpty()) {
        val forApp = call.getDataFromAny("forApp")?.toBoolean() ?: false
        val user = User.byMail(mail)
        if (user == null || !user.active) {
            call.respond(ReturnCode.BAD_REQUEST)
            return
        }
        val otp = user.createOTP(forApp)
        Mail.sendMailAsync(
            user.mail, user.fullName, "Login to expoll", "Here is your OTP for logging in on the expoll website: \n\t" +
                    otp.otp +
                    "\n alternatively you can click this link \n" +
                    URLBuilder.buildLoginLink(call, otp.otp)
        )
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
        val session = otp.createSessionAndDeleteSelf(call.request.headers["User-Agent"] ?: "unknown")
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
    call.respondRedirect(DeepLinkBuilder.buildLoginLink(call, otp.otp))
}