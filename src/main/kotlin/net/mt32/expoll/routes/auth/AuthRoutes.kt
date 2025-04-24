package net.mt32.expoll.routes.auth

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import net.mt32.expoll.auth.ExpollJWTCookie
import net.mt32.expoll.auth.JWTSessionPrincipal
import net.mt32.expoll.auth.normalAuth
import net.mt32.expoll.entities.Session
import net.mt32.expoll.commons.helper.ReturnCode
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.authRoutes(){
    route("auth"){
        simpleAuthRoutes()
        webauthnRoutes()
        oidcRoutes()
        authenticate(normalAuth) {
            delete("logout") {
                logout(call)
            }
            delete("logoutAll") {
                logoutAll(call)
            }
            get("loggedIn"){
                call.respond(ReturnCode.OK)
            }
        }
    }
}

private suspend fun logoutAll(call: ApplicationCall){
    val principal = call.principal<JWTSessionPrincipal>()
    if(principal == null){
        call.respond(ReturnCode.UNAUTHORIZED)
        return
    }
    principal.user.sessions.forEach {
        it.delete()
    }
    call.respond(ReturnCode.OK)
}

@Serializable
data class LogoutRequest(
    val nonce: Long? = null
)

private suspend fun logout(call: ApplicationCall){
    val principal = call.principal<JWTSessionPrincipal>()
    if(principal == null){
        call.respond(ReturnCode.UNAUTHORIZED)
        return
    }
    val (nonce) = call.receive<LogoutRequest>()
    if(nonce != null){
        val session = Session.fromNonce(nonce)
        if(session?.userID != principal.userID || principal.user.superAdminOrAdmin) return
        session.delete()
        call.respond(ReturnCode.OK)
    }else {
        transaction {
            Session.deleteWhere { Session.nonce eq principal.session.nonce }
        }
        call.sessions.clear<ExpollJWTCookie>()
        call.respond(ReturnCode.OK)
    }
}