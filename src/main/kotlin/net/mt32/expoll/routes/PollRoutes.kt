package net.mt32.expoll.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.mt32.expoll.auth.BasicSessionPrincipal
import net.mt32.expoll.entities.Poll
import net.mt32.expoll.helper.ReturnCode
import net.mt32.expoll.helper.getDataFromAny
import net.mt32.expoll.serializable.responses.asPollListResponse
import net.mt32.expoll.tPollID

fun Route.pollRoutes() {
    route("/poll") {
        get{
            getPolls(call)
        }
//TODO add poll routes
    }
}

private suspend fun editPoll(call: ApplicationCall) {
    TODO()
}

private suspend fun createPoll(call: ApplicationCall) {
    TODO()
}

private suspend fun getPolls(call: ApplicationCall) {
    val principal = call.principal<BasicSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.INTERNAL_SERVER_ERROR)
        return
    }
    val pollID = call.getDataFromAny("pollID")
    if (pollID != null) {
        getDetailedPoll(call, pollID)
    }else
        getPollList(call)
}

private suspend fun getPollList(call: ApplicationCall) {
    val principal = call.principal<BasicSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.INTERNAL_SERVER_ERROR)
        return
    }
    call.respond(principal.user.polls.asPollListResponse())
}

private suspend fun getDetailedPoll(call: ApplicationCall, pollID: tPollID) {
    val principal = call.principal<BasicSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.INTERNAL_SERVER_ERROR)
        return
    }
    val poll = Poll.fromID(pollID)
    if (poll == null) {
        call.respond(ReturnCode.INVALID_PARAMS)
        return
    }
    call.respond(poll.asDetailedPoll())
}