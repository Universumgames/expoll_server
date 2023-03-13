package net.mt32.expoll.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import net.mt32.expoll.Mail
import net.mt32.expoll.auth.*
import net.mt32.expoll.config
import net.mt32.expoll.entities.MailRule
import net.mt32.expoll.entities.User
import net.mt32.expoll.helper.ReturnCode
import net.mt32.expoll.helper.ServerTimings
import net.mt32.expoll.helper.addServerTiming
import net.mt32.expoll.helper.getDataFromAny
import net.mt32.expoll.serializable.request.VoteChange
import net.mt32.expoll.serializable.responses.CreateUserResponse
import net.mt32.expoll.serializable.responses.UserDataResponse
import net.mt32.expoll.serializable.responses.UserPersonalizeResponse
import net.mt32.expoll.serializable.responses.asSimpleList

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
            get {
                getUserData(call)
            }
            get("/personalizeddata"){
                getPersonalizedData(call)
            }
            // TODO implement delete user endpoint
            // TODO implement delete user confirmation endpoint
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

    if(captcha != null){
        val verified = verifyGoogleCAPTCHA(captcha)
        if(verified.score < 0.5){
            call.respond(ReturnCode.CAPTCHA_INVALID)
            return
        }
    }
    if(appAttest != null){
        val verified = verifyAppAttest(appAttest)
        if(!verified){
            call.respond(ReturnCode.CAPTCHA_INVALID)
            return
        }
    }

    val user = User(username, firstName, lastName, mail, admin = false)
    user.save()

    val session = user.createSession()

    val port = config.frontEndPort
    val protocol = call.request.local.scheme
    Mail.sendMail(
        user.mail, "Thank you for registering in expoll",
        "Thank you for creating an account at over at expoll (" +
                protocol +
                "://" +
                config.loginLinkURL +
                (if (port == 80 || port == 443) "" else ":$port") +
                ")"
    )

    call.sessions.set(ExpollCookie(session.loginkey))
    call.respond(CreateUserResponse(session.loginkey))
}

private suspend fun getUserData(call: ApplicationCall) {
    val principal = call.principal<BasicSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.INTERNAL_SERVER_ERROR)
        return
    }
    val user = principal.user

    val simpleUserResponse = UserDataResponse(
        user.id,
        user.username,
        user.firstName,
        user.lastName,
        user.mail,
        user.active,
        principal.admin || principal.superUser,
        user.polls.asSimpleList()
    )
    call.respond(simpleUserResponse)
}

private suspend fun getPersonalizedData(call: ApplicationCall){
    val timings = ServerTimings("user.basic", "Gather user and session data")
    val principal = call.principal<BasicSessionPrincipal>()
    if(principal == null){
        call.respond(ReturnCode.INTERNAL_SERVER_ERROR)
        return
    }

    val user = principal.user

    timings.startNewTiming("user.polls", "Gather polls")
    val polls = user.polls.map { it.asSimplePoll() }
    timings.startNewTiming("user.votes", "Gather votes")
    val votes = user.votes.map { VoteChange(it.pollID, it.optionID, it.votedFor.id) }
    timings.startNewTiming("user.sessions", "Gather sessions")
    val sessions = user.sessions.map { it.asSafeSession(principal.loginKey) }
    timings.startNewTiming("user.auths", "Gather authenticators")
    val auths = user.authenticators.map { it.asSimpleAuthenticator() }


    val personalizedData = UserPersonalizeResponse(
        user.id,
        user.username,
        user.firstName,
        user.lastName,
        user.mail,
        polls,
        votes,
        sessions,
        user.notes,
        user.active,
        user.admin,
        user.superAdmin,
        auths
    )
    call.addServerTiming(timings)
    call.respond(personalizedData)
}

private suspend fun createChallenge(call: ApplicationCall) {
    val userName = call.getDataFromAny("username")
    val mail = call.getDataFromAny("mail")
    call.respondText { "challenge${userName}${mail}" }
}