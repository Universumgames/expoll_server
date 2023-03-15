package net.mt32.expoll.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.mt32.expoll.VoteValue
import net.mt32.expoll.auth.BasicSessionPrincipal
import net.mt32.expoll.entities.Poll
import net.mt32.expoll.entities.Vote
import net.mt32.expoll.helper.ReturnCode
import net.mt32.expoll.helper.UnixTimestamp
import net.mt32.expoll.helper.startNewTiming
import net.mt32.expoll.notification.ExpollNotification
import net.mt32.expoll.notification.ExpollNotificationType
import net.mt32.expoll.notification.sendNotification
import net.mt32.expoll.serializable.request.VoteChange

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
    call.startNewTiming("vote.parse", "Parse request data")
    val voteChange: VoteChange = call.receive()

    if (voteChange.userID != null && !principal.admin) {
        call.respond(ReturnCode.UNAUTHORIZED)
        return
    }

    call.startNewTiming("poll.load", "Load basic poll data")

    val poll = Poll.fromID(voteChange.pollID)
    val votedForEnum = VoteValue.values().find { it.id == voteChange.votedFor }
    if (poll == null ||
        votedForEnum == null ||
        !poll.options.map { it.id }.contains(voteChange.optionID) ||
        (votedForEnum == VoteValue.MAYBE && !poll.allowsMaybe)
    ) {
        call.respond(ReturnCode.NOT_ACCEPTABLE)
        return
    }

    call.startNewTiming("vote.check", "Check that votes count does not exceed maximum")

    if (!poll.allowsEditing) {
        call.respond(ReturnCode.CHANGE_NOT_ALLOWED)
        return
    }

    val userIDToUse = voteChange.userID ?: principal.userID

    val voteCount = Vote.fromUserPoll(userIDToUse, voteChange.pollID)
        .filter { it.votedFor == VoteValue.MAYBE || it.votedFor == VoteValue.YES }.size
    if (voteCount > poll.maxPerUserVoteCount && poll.maxPerUserVoteCount != -1) {
        call.respond(ReturnCode.CHANGE_NOT_ALLOWED)
        return
    }

    call.startNewTiming("vote.save", "Save data to database")

    Vote.setVote(userIDToUse, voteChange.pollID, voteChange.optionID, votedForEnum)
    poll.updatedTimestamp = UnixTimestamp.now()
    poll.save()

    sendNotification(ExpollNotification(ExpollNotificationType.VoteChange, poll.id, userIDToUse))
    call.respond(ReturnCode.OK)
}