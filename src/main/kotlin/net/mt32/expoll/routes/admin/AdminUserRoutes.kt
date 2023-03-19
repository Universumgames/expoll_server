package net.mt32.expoll.routes.admin

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.mt32.expoll.Mail
import net.mt32.expoll.auth.BasicSessionPrincipal
import net.mt32.expoll.config
import net.mt32.expoll.entities.User
import net.mt32.expoll.helper.ReturnCode
import net.mt32.expoll.helper.getDataFromAny
import net.mt32.expoll.helper.startNewTiming
import net.mt32.expoll.serializable.admin.request.AdminCreateUserRequest
import net.mt32.expoll.serializable.admin.request.AdminEditUserRequest
import net.mt32.expoll.serializable.admin.responses.UserInfo
import net.mt32.expoll.serializable.admin.responses.UserListResponse

internal fun Route.adminUserRoutes() {
    route("/users") {
        get {
            getUsers(call)
        }
        post {
            createUser(call)
        }
        put {
            editUser(call)
        }
        delete {
            deleteUser(call)
        }
    }
}

private suspend fun getUsers(call: ApplicationCall) {
    val principal = call.principal<BasicSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.INTERNAL_SERVER_ERROR)
        return
    }
    call.startNewTiming("users.load", "Load all users")
    val users = User.all()

    call.startNewTiming("users.transform", "Transform user list to simple")
    val userInfos = users.map { user ->
        UserInfo(
            user.id,
            user.username,
            user.firstName,
            user.lastName,
            user.mail,
            user.admin || user.mail.equals(config.superAdminMail, ignoreCase = true),
            user.mail.equals(config.superAdminMail, ignoreCase = true),
            user.active
        )
    }
    call.respond(UserListResponse(userInfos, users.size))
}

private suspend fun createUser(call: ApplicationCall) {
    val createUserRequest: AdminCreateUserRequest = call.receive()

    // check if user exists
    if (User.byMail(createUserRequest.mail) != null || User.byUsername(createUserRequest.username) != null) {
        call.respond(ReturnCode.USER_EXISTS)
        return
    }

    val user = User(
        createUserRequest.username,
        createUserRequest.firstName,
        createUserRequest.lastName,
        createUserRequest.mail,
        active = true,
        admin = false
    )

    user.save()
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

    call.respond(ReturnCode.OK)
}

private suspend fun editUser(call: ApplicationCall) {
    val editUserRequest: AdminEditUserRequest = call.receive()

    val user = User.loadFromID(editUserRequest.userID)
    if (user == null) {
        call.respond(ReturnCode.INVALID_PARAMS)
        return
    }

    user.firstName = editUserRequest.firstName ?: user.firstName
    user.lastName = editUserRequest.lastName ?: user.lastName
    user.mail = editUserRequest.mail ?: user.mail

    user.save()
    call.respond(ReturnCode.OK)
}

private suspend fun deleteUser(call: ApplicationCall) {
    val userID = call.getDataFromAny("userID")
    if (userID == null) {
        call.respond(ReturnCode.MISSING_PARAMS)
        return
    }
    val user = User.loadFromID(userID)
    if (user == null) {
        call.respond(ReturnCode.INVALID_PARAMS)
        return
    }
    user.delete()
    call.respond(ReturnCode.OK)
}