package net.mt32.expoll.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import net.mt32.expoll.Mail
import net.mt32.expoll.auth.*
import net.mt32.expoll.config
import net.mt32.expoll.entities.MailRule
import net.mt32.expoll.entities.User
import net.mt32.expoll.helper.ReturnCode
import net.mt32.expoll.helper.getDataFromAny
import net.mt32.expoll.helper.startNewTiming
import net.mt32.expoll.serializable.request.CreateUserRequest
import net.mt32.expoll.serializable.request.EditUserRequest
import net.mt32.expoll.serializable.request.VoteChange
import net.mt32.expoll.serializable.responses.CreateUserResponse
import net.mt32.expoll.serializable.responses.UserDataResponse
import net.mt32.expoll.serializable.responses.UserPersonalizeResponse

fun Route.userRoutes() {
    route("/user") {
        get("createChallenge") {
            createChallenge(call)
        }
        // create user
        post {
            createUser(call)
        }

        get("appCreateRedirect") {
            call.respondRedirect(URLBuilder("http://" + config.loginLinkURL + "/#/login?mail=" + call.parameters["mail"]).build())
        }
        authenticate(normalAuth) {
            get {
                getUserData(call)
            }
            get("/personalizeddata") {
                getPersonalizedData(call)
            }
            put{
                editUser(call)
            }
            // TODO implement delete user endpoint
            // TODO implement delete user confirmation endpoint
        }
    }
}

private suspend fun createUser(call: ApplicationCall) {
    call.startNewTiming("user.create.parse", "Parse create user data")
    val createUserRequest: CreateUserRequest = call.receive()
    val firstName = createUserRequest.firstName
    val lastName = createUserRequest.lastName
    val mail = createUserRequest.mail
    val username = createUserRequest.username
    val captcha = createUserRequest.captcha
    val appAttest = createUserRequest.appAttest

    call.startNewTiming("user.create.checks", "Check that dat complies with policies")
    // null check
    if (captcha == null && appAttest == null) {
        call.respond(ReturnCode.MISSING_PARAMS)
        return
    }
    // check user does not exist already
    if (User.byMail(mail) != null || User.byUsername(username) != null) {
        call.respond(ReturnCode.USER_EXISTS)
        return
    }

    // check if mail is allowed
    if (!MailRule.mailIsAllowed(mail)) {
        call.respond(ReturnCode.NOT_ACCEPTABLE)
        return
    }

    call.startNewTiming("captcha.validate", "Validate Captcha or app attest")

    if (captcha != null) {
        val verified = verifyGoogleCAPTCHA(captcha)
        if (verified.score < 0.5) {
            call.respond(ReturnCode.CAPTCHA_INVALID)
            return
        }
    } else if (appAttest != null) {
        val verified = verifyAppAttest(appAttest)
        if (!verified) {
            call.respond(ReturnCode.CAPTCHA_INVALID)
            return
        }
    } else {
        call.respond(ReturnCode.INVALID_PARAMS)
        return
    }

    call.startNewTiming("user.create", "Create User and save to database")
    val user = User(username, firstName, lastName, mail, admin = false)
    user.save()

    call.startNewTiming("session.create", "Create new Session")
    val session = user.createSessionFromScratch()

    call.startNewTiming("user.create.welcomeMail", "Send welcome mail")
    val port = config.frontEndPort
    val protocol = call.request.local.scheme
    Mail.sendMailAsync(
        user.mail, "Thank you for registering in expoll",
        "Thank you for creating an account at over at expoll (" +
                protocol +
                "://" +
                config.loginLinkURL +
                (if (port == 80 || port == 443) "" else ":$port") +
                ")"
    )

    val jwt = session.getJWT()
    call.sessions.set(ExpollJWTCookie(jwt))
    call.respond(CreateUserResponse(jwt))
}

private suspend fun getUserData(call: ApplicationCall) {
    val principal = call.principal<JWTSessionPrincipal>()
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
        principal.admin || principal.superAdmin
    )
    call.respond(simpleUserResponse)
}

private suspend fun getPersonalizedData(call: ApplicationCall) {
    call.startNewTiming("user.basic", "Gather user and session data")
    val principal = call.principal<JWTSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.INTERNAL_SERVER_ERROR)
        return
    }

    val user = principal.user

    call.startNewTiming("user.polls", "Gather polls")
    val polls = user.polls.map { it.asSimplePoll() }
    call.startNewTiming("user.votes", "Gather votes")
    val votes = user.votes.map { VoteChange(it.pollID, it.optionID, it.votedFor.id) }
    call.startNewTiming("user.sessions", "Gather sessions")
    val sessions = user.sessions.map { it.asSafeSession(principal.session) }
    call.startNewTiming("user.auths", "Gather authenticators")
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
    call.respond(personalizedData)
}

private suspend fun createChallenge(call: ApplicationCall) {
    val userName = call.getDataFromAny("username")
    val mail = call.getDataFromAny("mail")
    call.respondText { "challenge${userName}${mail}" }
}

private suspend fun editUser(call: ApplicationCall){
    val principal = call.principal<JWTSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.INTERNAL_SERVER_ERROR)
        return
    }
    val editRequest = call.receive<EditUserRequest>()
    val user = principal.user
    val username = editRequest.username
    if(username != null){
        val foundUser = User.byUsername(username)
        if(foundUser != null && foundUser.id != user.id){
            call.respond(ReturnCode.INVALID_PARAMS)
            return
        }
        user.username = username
    }
    user.lastName = editRequest.lastName ?: user.lastName
    user.firstName = editRequest.firstName ?: user.firstName
    user.save()
    call.respond(ReturnCode.OK)
}