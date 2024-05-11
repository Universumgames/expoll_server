package net.mt32.expoll.helper

import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import net.mt32.expoll.entities.User

suspend fun ApplicationCall.respondWithOTPRedirect(user: User, forApp: Boolean = false, isNewUser: Boolean = false) {
    val otp = user.createOTP(forApp = forApp)
    val url = URLBuilder.buildLoginLink(this, user, otp, isNewUser = isNewUser)
    this.respondRedirect(url)
}

@Serializable
data class OTPResponse(val otp: String, val isNewUser: Boolean)

suspend fun ApplicationCall.respondWithOTPJSON(user: User, isNewUser: Boolean = false) {
    val otp = user.createOTP(false)
    this.respond(OTPResponse(otp.otp, isNewUser))
}