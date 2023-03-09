package net.mt32.expoll.routes.auth

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import net.mt32.expoll.ExpollCookie
import net.mt32.expoll.Mail
import net.mt32.expoll.entities.Session
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

    if (loginKey == null && mail == null) {
        call.respond(ReturnCode.MISSING_PARAMS)
        return
    }

    if (mail != null) {
        val user = User.byMail(mail)
        if (user == null) {
            call.respond(ReturnCode.BAD_REQUEST)
            return
        }
        val session = user.createSession()
        Mail.sendMail(
            user.mail, "Login to expoll", "Here is you login key for logging in on the expoll website: \n\t" +
                    session.loginkey +
                    "\n alternatively you can click this link \n" +
                    urlBuilder(call, session.loginkey)
        )
        call.respond(ReturnCode.OK)
    }

    if(loginKey != null) {
        val session = Session.fromLoginKey(loginKey)
        if(session == null){
            call.respond(ReturnCode.UNAUTHORIZED)
            return
        }
        call.sessions.set(ExpollCookie(session.loginkey))
        call.respond(ReturnCode.OK)
    }
    call.respond(ReturnCode.INTERNAL_SERVER_ERROR)
}