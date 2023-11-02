package net.mt32.expoll.routes.admin

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.mt32.expoll.entities.Poll
import net.mt32.expoll.serializable.admin.responses.AdminPollResponse

internal fun Route.adminPollRoutes() {
    get("/polls") {
        getPolls(call)
    }
}

private suspend fun getPolls(call: ApplicationCall) {
    val polls = Poll.all().map { it.asSimplePoll(null) }.sortedBy { -it.lastUpdated }
    call.respond(AdminPollResponse(polls, polls.size))
}