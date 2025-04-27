package net.mt32.expoll.routes.admin

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.mt32.expoll.commons.serializable.admin.request.AdminPollListRequest
import net.mt32.expoll.commons.serializable.admin.responses.AdminPollResponse
import net.mt32.expoll.entities.Poll
import net.mt32.expoll.plugins.query

internal fun Route.adminPollRoutes() {
    route("/polls") {
        get {
            getPolls(call)
        }
        query {
            getPolls(call)
        }
    }
}

private suspend fun getPolls(call: ApplicationCall) {
    val adminListRequest: AdminPollListRequest = call.receiveNullable() ?: AdminPollListRequest()
    val polls = Poll.all(adminListRequest.limit, adminListRequest.offset, adminListRequest.searchParameters)
        .map { it.asSimplePoll(null) }.sortedBy { -it.lastUpdated }
    call.respond(AdminPollResponse(polls, polls.size))
}

