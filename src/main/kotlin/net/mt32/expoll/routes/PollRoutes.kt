package net.mt32.expoll.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.mt32.expoll.PollType
import net.mt32.expoll.auth.BasicSessionPrincipal
import net.mt32.expoll.config
import net.mt32.expoll.entities.Poll
import net.mt32.expoll.entities.PollUserNote
import net.mt32.expoll.helper.ReturnCode
import net.mt32.expoll.helper.getDataFromAny
import net.mt32.expoll.serializable.request.CreatePollRequest
import net.mt32.expoll.serializable.request.EditPollRequest
import net.mt32.expoll.serializable.responses.PollCreatedResponse
import net.mt32.expoll.serializable.responses.asPollListResponse
import net.mt32.expoll.tPollID

fun Route.pollRoutes() {
    route("/poll") {
        get {
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
    }
}

private suspend fun editPoll(call: ApplicationCall) {
    val principal = call.principal<BasicSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.INTERNAL_SERVER_ERROR)
        return
    }
    val editPollRequest: EditPollRequest?
    try {
        editPollRequest = call.receive()
    } catch (e: BadRequestException) {
        call.respond(ReturnCode.MISSING_PARAMS)
        return
    }

    val poll = Poll.fromID(editPollRequest.pollID)
    if (poll == null) {
        call.respond(ReturnCode.INVALID_PARAMS)
        return
    }
    if (principal.user != poll.admin && !principal.user.mail.equals(config.superAdminMail, ignoreCase = true)) {
        call.respond(ReturnCode.UNAUTHORIZED)
        return
    }

    // TODO implement delete poll

    // set basic settings
    poll.name = editPollRequest.name ?: poll.name
    poll.description = editPollRequest.description ?: poll.description
    poll.maxPerUserVoteCount = editPollRequest.maxPerUserVoteCount ?: poll.maxPerUserVoteCount
    poll.allowsMaybe = editPollRequest.allowsMaybe ?: poll.allowsMaybe
    poll.allowsEditing = editPollRequest.allowsEditing ?: poll.allowsEditing

    // remove users
    poll.users = poll.users.filterNot { user -> editPollRequest.userRemove.contains(user.id) }

    // add/remove options
    editPollRequest.options.forEach { cOption ->
        if (cOption.id == null) { // new option
            poll.addOption(cOption)
        } else {
            val option = poll.options.find { it.id == cOption.id }
            option?.delete()
        }
    }

    // update notes
    editPollRequest.notes.distinctBy { it.userID }.forEach { note ->
        if (note.note == null) return@forEach
        val dbNote = poll.notes.find { it.userID == note.userID } ?: PollUserNote(note.userID, poll.id, note.note)
        dbNote.note = note.note
        dbNote.save()
    }

    poll.save()
    call.respond(ReturnCode.OK)
}

private suspend fun leavePoll(call: ApplicationCall) {
    val principal = call.principal<BasicSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.INTERNAL_SERVER_ERROR)
        return
    }
    val pollID = call.getDataFromAny("pollID")
    if (pollID == null) {
        call.respond(ReturnCode.MISSING_PARAMS)
        return
    }
    // TODO remove votes
    principal.user.polls = principal.user.polls.filterNot { it.id == pollID }
    principal.user.save()
    call.respond(ReturnCode.OK)
}

private suspend fun joinPoll(call: ApplicationCall) {
    val principal = call.principal<BasicSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.INTERNAL_SERVER_ERROR)
        return
    }
    val pollID = call.getDataFromAny("pollID")
    if (pollID == null) {
        call.respond(ReturnCode.MISSING_PARAMS)
        return
    }
    val poll = Poll.fromID(pollID)
    if (poll == null) {
        call.respond(ReturnCode.INVALID_PARAMS)
        return
    }
    principal.user.addPoll(poll.id)
    call.respond(ReturnCode.OK)
}

private suspend fun createPoll(call: ApplicationCall) {
    val principal = call.principal<BasicSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.INTERNAL_SERVER_ERROR)
        return
    }

    val createPollRequest: CreatePollRequest?
    try {
        createPollRequest = call.receive()
    } catch (e: BadRequestException) {
        call.respond(ReturnCode.MISSING_PARAMS)
        return
    }

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
        createPollRequest.allowsEditing
    )


    poll.save()
    createPollRequest.options.forEach { option ->
        poll.addOption(option)
    }
    principal.user.addPoll(poll.id)

    call.respond(PollCreatedResponse(poll.id))
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
    } else
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