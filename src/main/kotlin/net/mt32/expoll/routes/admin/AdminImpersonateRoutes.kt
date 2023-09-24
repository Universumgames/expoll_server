package net.mt32.expoll.routes.admin

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import net.mt32.expoll.auth.ExpollJWTCookie
import net.mt32.expoll.auth.JWTSessionPrincipal
import net.mt32.expoll.auth.adminAuth
import net.mt32.expoll.auth.normalAuth
import net.mt32.expoll.entities.User
import net.mt32.expoll.helper.ReturnCode
import net.mt32.expoll.helper.getDataFromAny

internal fun Route.adminImpersonateRoutes() {
    route("/") {
        authenticate(adminAuth) {
            post("/impersonate") {
                impersonate(call)
            }
        }
        authenticate(normalAuth) {
            get("/isImpersonating") {
                isImpersonating(call)
            }
            post("/unImpersonate") {
                unImpersonate(call)
            }
        }
    }
}


private suspend fun impersonate(call: ApplicationCall) {
    val principal = call.principal<JWTSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.INTERNAL_SERVER_ERROR)
        return
    }
    val session = call.sessions.get<ExpollJWTCookie>()
    val impersonateID = call.getDataFromAny("impersonateID")
    if (impersonateID == null) {
        call.respond(ReturnCode.MISSING_PARAMS)
        return
    }
    val impersonateUser = User.loadFromID(impersonateID)
    if (impersonateUser == null) {
        call.respond(ReturnCode.INVALID_PARAMS)
        return
    }
    if (impersonateUser.superAdmin) {
        call.respond(ReturnCode.UNAUTHORIZED)
        return
    }

    val newSession = impersonateUser.createSessionFromScratch()
    val newJwt = newSession.getJWT()
    call.sessions.set(ExpollJWTCookie(newJwt, session?.jwt))
    call.respond(newJwt)
}

private suspend fun isImpersonating(call: ApplicationCall) {
    val principal = call.principal<JWTSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.INTERNAL_SERVER_ERROR)
        return
    }
    val user = User.loadFromID(principal.originalUserID ?: "")
    if (user == null) {
        call.respond(ReturnCode.INVALID_PARAMS)
        return
    }
    call.respond(principal.user.asUserInfo())
}

private suspend fun unImpersonate(call: ApplicationCall) {
    val session = call.sessions.get<ExpollJWTCookie>()
    if (session == null) {
        call.respond(ReturnCode.MISSING_PARAMS)
        return
    }
    val origKey = session.originalJWT
    if (origKey == null) {
        call.respond(ReturnCode.INVALID_PARAMS)
        return
    }
    call.sessions.set(ExpollJWTCookie(origKey))
    call.respond(ReturnCode.OK)
}