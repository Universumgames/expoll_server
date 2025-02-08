package net.mt32.expoll.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import net.mt32.expoll.PollType
import net.mt32.expoll.VoteValue
import net.mt32.expoll.config
import net.mt32.expoll.entities.Poll
import net.mt32.expoll.entities.PollUserNote
import net.mt32.expoll.entities.User
import net.mt32.expoll.entities.Vote
import net.mt32.expoll.entities.interconnect.UserPolls
import net.mt32.expoll.helper.ReturnCode
import net.mt32.expoll.helper.startNewTiming
import net.mt32.expoll.notification.ExpollNotificationHandler
import net.mt32.expoll.plugins.getAuthPrincipal
import net.mt32.expoll.plugins.query
import net.mt32.expoll.serializable.request.BasicPollOperation
import net.mt32.expoll.serializable.request.CreatePollRequest
import net.mt32.expoll.serializable.request.EditPollRequest
import net.mt32.expoll.serializable.request.PollRequest
import net.mt32.expoll.serializable.request.search.PollSearchParameters
import net.mt32.expoll.serializable.responses.PollCreatedResponse
import net.mt32.expoll.serializable.responses.asPollListResponse
import net.mt32.expoll.tPollID

fun Route.pollRoutes() {
    route("/poll") {
        get {
            getPolls(call)
        }
        query {
            getPolls(call)
        }
        post {
            createPoll(call)
        }
        put {
            editPoll(call)
        }
        post("/leave") {
            leavePoll(call)
        }
        post("/join") {
            joinPoll(call)
        }
        post("/hide") {
            hidePoll(call)
        }
        get("/availableSearch") {
            getAvailableSearchParameters(call)
        }
    }
}

private suspend fun editPoll(call: ApplicationCall) {
    val principal = call.getAuthPrincipal()
    call.startNewTiming("poll.edit.parse", "Parsing poll edit request")
    val editPollRequest: EditPollRequest = call.receive()

    call.startNewTiming("poll.load.basic", "load basic poll data from database")
    val poll = Poll.fromID(editPollRequest.pollID)
    if (poll == null) {
        call.respond(ReturnCode.INVALID_PARAMS)
        return
    }
    if (principal.user != poll.admin && !principal.user.admin && !principal.user.mail.equals(config.superAdminMail, ignoreCase = true)) {
        call.respond(ReturnCode.UNAUTHORIZED)
        return
    }

    if (!poll.allowsEditing && editPollRequest.allowsEditing == false) {
        call.respond(ReturnCode.CHANGE_NOT_ALLOWED)
        return
    }

    call.startNewTiming("poll.edit.set", "Update poll variables")
    // set basic settings
    poll.name = editPollRequest.name ?: poll.name
    poll.description = editPollRequest.description ?: poll.description
    poll.maxPerUserVoteCount = editPollRequest.maxPerUserVoteCount ?: poll.maxPerUserVoteCount
    poll.allowsMaybe = editPollRequest.allowsMaybe ?: poll.allowsMaybe
    poll.allowsEditing = editPollRequest.allowsEditing ?: poll.allowsEditing
    poll.privateVoting = editPollRequest.privateVoting ?: poll.privateVoting

    if (editPollRequest.allowsEditing == false) {
        ExpollNotificationHandler.sendPollEdit(poll)
    }

    // remove users
    editPollRequest.userRemove.forEach {
        poll.removeUser(it)
        val user = User.loadFromID(it)
        if (user != null) ExpollNotificationHandler.sendPollLeave(poll, user)
    }

    // add users
    editPollRequest.userAdd.forEach {
        var user = User.loadFromID(it)
        if (user == null) user = User.byUsername(it)
        if (user == null) user = User.byMail(it)
        if (user == null) return@forEach
        poll.addUser(user.id)
        ExpollNotificationHandler.sendPollJoin(poll, user)
    }

    // add/remove options
    editPollRequest.options.forEach { cOption ->
        val option = poll.options.find { it.id == cOption.id }
        if (cOption.id == null || option == null) { // new option
            poll.addOption(cOption)
        } else {
            option.delete()
            Vote.fromPollOption(poll.id, option.id).forEach { it.delete() }
        }
    }

    // update notes
    editPollRequest.notes.distinctBy { it.userID }.forEach { note ->
        if (note.note == null) return@forEach
        val dbNote = poll.notes.find { it.userID == note.userID } ?: PollUserNote(note.userID, poll.id, note.note)
        dbNote.note = note.note
        dbNote.save()
    }

    call.startNewTiming("poll.save", "Save poll to database")

    poll.save()
    ExpollNotificationHandler.sendPollEdit(poll)

    if (editPollRequest.delete == true) {
        poll.delete()
        ExpollNotificationHandler.sendPollDelete(poll)
    }
    call.respond(ReturnCode.OK)
}

private suspend fun leavePoll(call: ApplicationCall) {
    val principal = call.getAuthPrincipal()
    val pollID = call.receive<BasicPollOperation>().pollID
    if (!Poll.exists(pollID)) {
        call.respond(ReturnCode.INVALID_PARAMS)
        return
    }

    val poll = Poll.fromID(pollID)
    if (poll == null) {
        call.respond(ReturnCode.INVALID_PARAMS)
        return
    }
    poll.removeUser(principal.userID)
    ExpollNotificationHandler.sendPollLeave(poll, principal.user)
    call.respond(ReturnCode.OK)
}

private suspend fun joinPoll(call: ApplicationCall) {
    val principal = call.getAuthPrincipal()
    val pollID = call.receive<BasicPollOperation>().pollID
    if (!Poll.exists(pollID)) {
        call.respond(ReturnCode.INVALID_PARAMS)
        return
    }
    val alreadyJoined = UserPolls.connectionExists(principal.userID, pollID)
    if (alreadyJoined) {
        call.respond(ReturnCode.OK)
        return
    }
    val poll = Poll.fromID(pollID)
    if (poll == null) {
        call.respond(ReturnCode.INVALID_PARAMS)
        return
    }
    poll.addUser(principal.userID)

    ExpollNotificationHandler.sendPollJoin(poll, principal.user)
    call.respond(ReturnCode.OK)
}

private suspend fun createPoll(call: ApplicationCall) {
    val principal = call.getAuthPrincipal()

    call.startNewTiming("poll.create.parse", "Parse poll creation data")
    val createPollRequest: CreatePollRequest = call.receive()


    call.startNewTiming("poll.create", "Create poll")
    val pollCount = principal.user.polls.count { it.adminID == principal.userID }
    if (pollCount >= config.maxPollCountPerUser && !principal.admin) {
        call.respond(ReturnCode.TOO_MANY_POLLS)
        return
    }

    val type = PollType.valueOf(createPollRequest.type)

    val poll = Poll.createPoll(
        principal.userID,
        createPollRequest.name,
        createPollRequest.description,
        type,
        createPollRequest.maxPerUserVoteCount,
        createPollRequest.allowsMaybe,
        createPollRequest.allowsEditing,
        createPollRequest.privateVoting,
        VoteValue.valueOf(createPollRequest.defaultVote) ?: VoteValue.UNKNOWN
    )


    call.startNewTiming("poll.save", "Save poll and options to database")
    poll.save()
    createPollRequest.options.forEach { option ->
        poll.addOption(option)
    }
    poll.addUser(principal.userID)

    call.respond(PollCreatedResponse(poll.id))
}

private suspend fun getPolls(call: ApplicationCall) {
    val principal = call.getAuthPrincipal()
    val pollRequest: PollRequest = call.receiveNullable() ?: PollRequest()
    val oldPollID = call.request.queryParameters["pollID"] // TODO browser does not work with query method
    if (pollRequest.pollID != null || oldPollID != null) {
        (pollRequest.pollID ?: oldPollID)?.let { getDetailedPoll(call, it) }
        getDetailedPoll(call, pollRequest.pollID ?: oldPollID!!)
    } else
        getPollList(call, pollRequest)
}

private suspend fun getPollList(call: ApplicationCall, pollRequest: PollRequest) {
    val principal = call.getAuthPrincipal()

    call.startNewTiming("polls.list", "Retrieve poll data from database")
    val searchParameters = pollRequest.searchParameters ?: PollSearchParameters()
    searchParameters.specialFilter = PollSearchParameters.SpecialFilter.JOINED
    val polls = if (pollRequest.searchParameters == null) principal.user.polls else Poll.all(
        searchParameters = searchParameters,
        forUserId = principal.userID
    )
    call.startNewTiming("polls.transform", "Transform poll data to simplified list format")
    val simplePolls = polls.asPollListResponse(principal.user)
    call.startNewTiming("polls.serialize", "Serialize data and prepare to send")
    call.respond(simplePolls)
}

private suspend fun getDetailedPoll(call: ApplicationCall, pollID: tPollID) {
    val principal = call.getAuthPrincipal()
    call.startNewTiming("polls.fetch", "Load basic poll data from database")
    val poll = Poll.fromID(pollID)
    if (poll == null) {
        call.respond(ReturnCode.INVALID_PARAMS)
        return
    }
    call.startNewTiming("poll.transform", "Transform poll data to detailed format")
    val detailedPoll = poll.asDetailedPoll(principal.user)
    call.respond(detailedPoll)
}

@Serializable
data class PollHideRequest(val pollID: tPollID, val hide: Boolean? = true)

private suspend fun hidePoll(call: ApplicationCall) {
    val principal = call.getAuthPrincipal()
    val pollHideRequest: PollHideRequest = call.receive()

    val poll = Poll.fromID(pollHideRequest.pollID)
    if (poll == null) {
        call.respond(ReturnCode.INVALID_PARAMS)
        return
    }

    UserPolls.hideFromListForUser(pollHideRequest.pollID, principal.userID, pollHideRequest.hide ?: true)
    call.respond(ReturnCode.OK)
}

private suspend fun getAvailableSearchParameters(call: ApplicationCall) {
    call.respond(PollSearchParameters.Descriptor())
}