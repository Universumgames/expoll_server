package net.mt32.expoll.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import net.mt32.expoll.BasicSessionPrincipal
import net.mt32.expoll.ExpollCookie
import net.mt32.expoll.entities.MailRule
import net.mt32.expoll.entities.User
import net.mt32.expoll.helper.ReturnCode
import net.mt32.expoll.helper.getDataFromAny
import net.mt32.expoll.normalAuth

fun Route.userRoutes() {
    route("/user") {
        get("createChallenge") {
            createChallenge(call)
        }
        // create user
        post {
            createUser(call)
        }
        authenticate(normalAuth) {

        }
        get("test") {
            val session = call.sessions.get<ExpollCookie>()
            val prince = call.principal<BasicSessionPrincipal>()
            print(prince)
            call.respondText { session?.loginKey ?: "not found" }
        }
    }
}

private suspend fun createUser(call: ApplicationCall) {
    val firstName = call.getDataFromAny("firstName")
    val lastName = call.getDataFromAny("lastName")
    val mail = call.getDataFromAny("mail")
    val username = call.getDataFromAny("username")
    val captcha = call.getDataFromAny("captcha")
    val appAttest = call.getDataFromAny("appAttest")

    // null check
    if (firstName == null || lastName == null || mail == null || username == null || (captcha == null && appAttest == null)) {
        call.respond(ReturnCode.MISSING_PARAMS)
        return
    }

    // TODO validate captcha

    // TODO validate app attest

    // check user does not exist already
    if (User.byMail(mail) == null || User.byUsername(username) == null) {
        call.respond(ReturnCode.USER_EXISTS)
        return
    }

    // check if mail is allowed
    if (!MailRule.mailIsAllowed(mail)) {
        call.respond(ReturnCode.NOT_ACCEPTABLE)
        return
    }
    val user = User(username, firstName, lastName, mail, admin = false)
    user.save()

    val session = user.createSession()
    // TODO send signup mail

}

private suspend fun createChallenge(call: ApplicationCall) {
    val userName = call.getDataFromAny("username")
    val mail = call.getDataFromAny("mail")
    call.respondText { "challenge${userName}${mail}" }
}