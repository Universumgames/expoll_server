package net.mt32.expoll.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.mt32.expoll.VoteValue
import net.mt32.expoll.auth.JWTSessionPrincipal
import net.mt32.expoll.entities.Poll
import net.mt32.expoll.entities.User
import net.mt32.expoll.entities.Vote
import net.mt32.expoll.helper.ReturnCode
import net.mt32.expoll.helper.UnixTimestamp
import net.mt32.expoll.helper.startNewTiming
import net.mt32.expoll.notification.ExpollNotificationHandler
import net.mt32.expoll.serializable.request.VoteChange

fun Route.voteRoutes() {
    route("vote") {
        post {
            voteRoute(call)
        }
    }
}

suspend fun voteRoute(call: ApplicationCall) {
    val principal = call.principal<JWTSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.INTERNAL_SERVER_ERROR)
        return
    }
    call.startNewTiming("vote.parse", "Parse request data")
    val voteChange: VoteChange = call.receive()

    if (voteChange.userID != null && !principal.admin && voteChange.userID != principal.userID) {
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
    val oldVote = Vote.fromUserPollOption(userIDToUse, voteChange.pollID, voteChange.optionID)?.votedFor

    val votes = Vote.fromUserPoll(userIDToUse, voteChange.pollID)
    val filteredVotes = votes
        .filter { it.votedFor.id >= 1 }.filter { it.optionID != voteChange.optionID }
    val voteCount = filteredVotes.size
    val newVoteAddedValue =
        if (votedForEnum.id >= 1) 1 else 0 // what would be be added to the count if the vote is added
    val newVoteCount =
        voteCount + newVoteAddedValue // the new count taking into consideration the new vote and the old vote
    if (newVoteCount > poll.maxPerUserVoteCount && poll.maxPerUserVoteCount != -1) {
        call.respond(ReturnCode.CHANGE_NOT_ALLOWED)
        return
    }

    call.startNewTiming("vote.save", "Save data to database")

    Vote.setVote(userIDToUse, voteChange.pollID, voteChange.optionID, votedForEnum)
    poll.updatedTimestamp = UnixTimestamp.now()
    poll.save()

    val changedUser = if(userIDToUse == principal.userID) principal.user else User.loadFromID(userIDToUse)
    if(changedUser != null) ExpollNotificationHandler.sendVoteChange(poll, changedUser, voteChange.optionID, oldVote, votedForEnum)
    //sendNotification(ExpollNotification(ExpollNotificationType.VoteChange, poll.id, userIDToUse))
    call.respond(ReturnCode.OK)
}