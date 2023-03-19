package net.mt32.expoll.routes.auth

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import net.mt32.expoll.Mail
import net.mt32.expoll.auth.ExpollCookie
import net.mt32.expoll.entities.LoginKeySession
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
    val loginKey = call.getDataFromAny("loginKey")
    val mail = call.getDataFromAny("mail")

    if (loginKey.isNullOrEmpty() && mail.isNullOrEmpty()) {
        call.respond(ReturnCode.MISSING_PARAMS)
        return
    }

    if (!mail.isNullOrEmpty()) {
        val user = User.byMail(mail)
        if (user == null) {
            call.respond(ReturnCode.BAD_REQUEST)
            return
        }
        val session = user.createSession()
        Mail.sendMail(
            user.mail, "Login to expoll", "Here is you login key for logging in on the expoll website: \n\t" +
                    session.loginKey +
                    "\n alternatively you can click this link \n" +
                    urlBuilder(call, session.loginKey)
        )
        call.respond(ReturnCode.OK)
        return
    }

    if(!loginKey.isNullOrEmpty()) {
        val loginKeySession = LoginKeySession.fromLoginKey(loginKey)
        if(loginKeySession == null){
            call.respond(ReturnCode.UNAUTHORIZED)
            return
        }
        call.sessions.set(ExpollCookie(loginKeySession.loginKey))
        call.respond(ReturnCode.OK)
        return
    }
    call.respond(ReturnCode.INTERNAL_SERVER_ERROR)
}