package net.mt32.expoll.routes.admin

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import net.mt32.expoll.auth.BasicSessionPrincipal
import net.mt32.expoll.auth.ExpollCookie
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
    val principal = call.principal<BasicSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.INTERNAL_SERVER_ERROR)
        return
    }
    val session = call.sessions.get<ExpollCookie>()
    if (session == null) {
        call.respond(ReturnCode.MISSING_PARAMS)
        return
    }
    if (session.originalLoginKey != null) {
        call.respond(ReturnCode.UNPROCESSABLE_ENTITY)
        return
    }
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

    val newSession = impersonateUser.createSession()
    call.sessions.set(ExpollCookie(newSession.loginkey, session.loginKey))
    call.respond(newSession.loginkey)
}

private suspend fun isImpersonating(call: ApplicationCall) {
    val principal = call.principal<BasicSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.INTERNAL_SERVER_ERROR)
        return
    }
    val session = call.sessions.get<ExpollCookie>()
    if (session == null) {
        call.respond(ReturnCode.MISSING_PARAMS)
        return
    }
    val origKey = session.originalLoginKey
    if (origKey == null) {
        call.respond(ReturnCode.INVALID_PARAMS)
        return
    }
    val user = User.loadFromLoginKey(origKey)
    if (user == null) {
        call.respond(ReturnCode.INVALID_PARAMS)
        return
    }
    call.respond(principal.user.asUserInfo())
}

private suspend fun unImpersonate(call: ApplicationCall) {
    val session = call.sessions.get<ExpollCookie>()
    if (session == null) {
        call.respond(ReturnCode.MISSING_PARAMS)
        return
    }
    val origKey = session.originalLoginKey
    if (origKey == null) {
        call.respond(ReturnCode.INVALID_PARAMS)
        return
    }
    call.sessions.set(ExpollCookie(origKey))
    call.respond(ReturnCode.OK)
}