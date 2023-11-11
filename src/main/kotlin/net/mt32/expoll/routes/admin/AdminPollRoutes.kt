package net.mt32.expoll.routes.admin

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.mt32.expoll.entities.Poll
import net.mt32.expoll.entities.PollSearchParameters
import net.mt32.expoll.serializable.admin.request.AdminPollListRequest
import net.mt32.expoll.serializable.admin.responses.AdminPollResponse

internal fun Route.adminPollRoutes() {
    route("/polls") {
        get {
            getPolls(call)
        }
        get("/availableSearch") {
            getAvailableSearchParameters(call)
        }
    }
}

private suspend fun getPolls(call: ApplicationCall) {
    val adminListRequest: AdminPollListRequest = call.receiveNullable() ?: AdminPollListRequest()
    val polls = Poll.all(adminListRequest.limit, adminListRequest.offset, adminListRequest.searchParameters).map { it.asSimplePoll(null) }.sortedBy { -it.lastUpdated }
    call.respond(AdminPollResponse(polls, polls.size))
}

private suspend fun getAvailableSearchParameters(call: ApplicationCall) {
    call.respond(PollSearchParameters.Descriptor())
}