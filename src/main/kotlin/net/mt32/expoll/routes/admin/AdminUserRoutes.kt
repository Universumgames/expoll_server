package net.mt32.expoll.routes.admin

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.mt32.expoll.auth.BasicSessionPrincipal
import net.mt32.expoll.config
import net.mt32.expoll.entities.User
import net.mt32.expoll.helper.ReturnCode
import net.mt32.expoll.serializable.admin.responses.UserInfo
import net.mt32.expoll.serializable.admin.responses.UserListResponse

internal fun Route.adminUserRoutes(){
    route("/users") {
        get {
            getUsers(call)
        }
        put{
            TODO("edit user")
        }
        delete {
            TODO("delete user")
        }
    }
    post("/createUser"){
        TODO("create user")
    }
}

private suspend fun getUsers(call: ApplicationCall) {
    val principal = call.principal<BasicSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.INTERNAL_SERVER_ERROR)
        return
    }
    val users = User.all().map { user ->
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
    call.respond(UserListResponse(users, users.size))
}