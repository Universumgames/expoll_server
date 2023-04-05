package net.mt32.expoll.routes.auth

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import net.mt32.expoll.Mail
import net.mt32.expoll.auth.ExpollJWTCookie
import net.mt32.expoll.entities.OTP
import net.mt32.expoll.entities.User
import net.mt32.expoll.helper.ReturnCode
import net.mt32.expoll.helper.getDataFromAny
import net.mt32.expoll.helper.urlBuilder

fun Route.simpleAuthRoutes() {
    route("simple") {
        post {
            simpleLoginRoute(call)
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
        val user = User.byMail(mail)
        if (user == null || !user.active) {
            call.respond(ReturnCode.BAD_REQUEST)
            return
        }
        val otp = user.createOTP()
        Mail.sendMail(
            user.mail, "Login to expoll", "Here is your OTP for logging in on the expoll website: \n\t" +
                    otp.otp +
                    "\n alternatively you can click this link \n" +
                    urlBuilder(call, otp.otp)
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
        call.sessions.set(ExpollJWTCookie(jwt))
        call.respondText(jwt)
        return
    }
    call.respond(ReturnCode.INTERNAL_SERVER_ERROR)
}