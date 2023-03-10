package net.mt32.expoll.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.mt32.expoll.VoteValue
import net.mt32.expoll.auth.BasicSessionPrincipal
import net.mt32.expoll.entities.Poll
import net.mt32.expoll.entities.Vote
import net.mt32.expoll.helper.ReturnCode
import net.mt32.expoll.helper.getDataFromAny

fun Route.voteRoutes() {
    route("vote") {
        post {
            voteRoute(call)
        }
    }
}

suspend fun voteRoute(call: ApplicationCall) {
    val principal = call.principal<BasicSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.INTERNAL_SERVER_ERROR)
        return
    }
    val pollID = call.getDataFromAny("pollID")
    val optionID = call.getDataFromAny("optionID")?.toIntOrNull()
    val votedFor = call.getDataFromAny("votedFor")?.toIntOrNull()
    val userID = call.getDataFromAny("userID")

    if (pollID == null || optionID == null || votedFor == null) {
        call.respond(ReturnCode.MISSING_PARAMS)
        return
    }

    if (userID != null && !principal.admin) {
        call.respond(ReturnCode.UNAUTHORIZED)
        return
    }

    val poll = Poll.fromID(pollID)
    val votedForEnum = VoteValue.values().find { it.id == votedFor }
    if (poll == null ||
        votedForEnum == null ||
        !poll.options.map { it.id }.contains(optionID) ||
        (votedForEnum == VoteValue.MAYBE && !poll.allowsMaybe)
    ) {
        call.respond(ReturnCode.NOT_ACCEPTABLE)
        return
    }

    val userIDToUse = userID ?: principal.userID

    val voteCount = Vote.fromUserPoll(principal.userID, pollID).filter { it.votedFor == VoteValue.MAYBE || it.votedFor == VoteValue.YES }.size
    if(voteCount > poll.maxPerUserVoteCount && poll.maxPerUserVoteCount != -1){
        call.respond(ReturnCode.CHANGE_NOT_ALLOWED)
        return
    }

    Vote.setVote(userIDToUse, pollID, optionID, votedForEnum)
    call.respond(ReturnCode.OK)
}