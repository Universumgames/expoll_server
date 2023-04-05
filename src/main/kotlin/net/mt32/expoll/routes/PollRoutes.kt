package net.mt32.expoll.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.mt32.expoll.PollType
import net.mt32.expoll.auth.JWTSessionPrincipal
import net.mt32.expoll.config
import net.mt32.expoll.entities.Poll
import net.mt32.expoll.entities.PollUserNote
import net.mt32.expoll.entities.User
import net.mt32.expoll.helper.ReturnCode
import net.mt32.expoll.helper.getDataFromAny
import net.mt32.expoll.helper.startNewTiming
import net.mt32.expoll.notification.ExpollNotification
import net.mt32.expoll.notification.ExpollNotificationType
import net.mt32.expoll.notification.sendNotification
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
    val principal = call.principal<JWTSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.INTERNAL_SERVER_ERROR)
        return
    }
    call.startNewTiming("poll.edit.parse", "Parsing poll edit request")
    val editPollRequest: EditPollRequest = call.receive()

    call.startNewTiming("poll.load.basic", "load basic poll data from database")
    val poll = Poll.fromID(editPollRequest.pollID)
    if (poll == null) {
        call.respond(ReturnCode.INVALID_PARAMS)
        return
    }
    if (principal.user != poll.admin && !principal.user.mail.equals(config.superAdminMail, ignoreCase = true)) {
        call.respond(ReturnCode.UNAUTHORIZED)
        return
    }

    if(!poll.allowsEditing && editPollRequest.allowsEditing == false){
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

    if(editPollRequest.allowsEditing == false){
        sendNotification(ExpollNotification(ExpollNotificationType.PollArchived, editPollRequest.pollID, null))
    }

    // remove users
    editPollRequest.userRemove.forEach {
        poll.removeUser(it)
        sendNotification(ExpollNotification(ExpollNotificationType.UserRemoved, editPollRequest.pollID, it))
    }

    // add users
    editPollRequest.userAdd.forEach {
        var user = User.loadFromID(it)
        if(user == null) user = User.byUsername(it)
        if(user == null) user = User.byMail(it)
        if(user == null) return@forEach
        poll.addUser(user.id)
        sendNotification(ExpollNotification(ExpollNotificationType.UserAdded, editPollRequest.pollID, user.id))
    }

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

    call.startNewTiming("poll.save", "Save poll to database")

    poll.save()
    sendNotification(ExpollNotification(ExpollNotificationType.PollEdited, editPollRequest.pollID, null))

    if (editPollRequest.delete == true) {
        poll.delete()
        sendNotification(ExpollNotification(ExpollNotificationType.PollDeleted, editPollRequest.pollID, null))
    }
    call.respond(ReturnCode.OK)
}

private suspend fun leavePoll(call: ApplicationCall) {
    val principal = call.principal<JWTSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.INTERNAL_SERVER_ERROR)
        return
    }
    val pollID = call.getDataFromAny("pollID")
    if (pollID == null) {
        call.respond(ReturnCode.MISSING_PARAMS)
        return
    }
    if (!Poll.exists(pollID)) {
        call.respond(ReturnCode.INVALID_PARAMS)
        return
    }
    principal.user.removePoll(pollID)
    sendNotification(ExpollNotification(ExpollNotificationType.UserRemoved, pollID, principal.userID))
    call.respond(ReturnCode.OK)
}

private suspend fun joinPoll(call: ApplicationCall) {
    val principal = call.principal<JWTSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.INTERNAL_SERVER_ERROR)
        return
    }
    val pollID = call.getDataFromAny("pollID")
    if (pollID == null) {
        call.respond(ReturnCode.MISSING_PARAMS)
        return
    }
    if (!Poll.exists(pollID)) {
        call.respond(ReturnCode.INVALID_PARAMS)
        return
    }
    principal.user.addPoll(pollID)
    sendNotification(ExpollNotification(ExpollNotificationType.UserAdded, pollID, principal.userID))
    call.respond(ReturnCode.OK)
}

private suspend fun createPoll(call: ApplicationCall) {
    val principal = call.principal<JWTSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.INTERNAL_SERVER_ERROR)
        return
    }

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
        createPollRequest.allowsEditing
    )


    call.startNewTiming("poll.save", "Save poll and options to database")
    poll.save()
    createPollRequest.options.forEach { option ->
        poll.addOption(option)
    }
    principal.user.addPoll(poll.id)

    call.respond(PollCreatedResponse(poll.id))
}

private suspend fun getPolls(call: ApplicationCall) {
    val principal = call.principal<JWTSessionPrincipal>()
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
    val principal = call.principal<JWTSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.INTERNAL_SERVER_ERROR)
        return
    }

    call.startNewTiming("polls.list", "Retrieve poll data from database")
    val polls = principal.user.polls
    call.startNewTiming("polls.transform", "Transform poll data to simplified list format")
    val simplePolls = polls.asPollListResponse()
    call.startNewTiming("polls.serialize", "Serialize data and prepare to send")
    call.respond(simplePolls)
}

private suspend fun getDetailedPoll(call: ApplicationCall, pollID: tPollID) {
    val principal = call.principal<JWTSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.INTERNAL_SERVER_ERROR)
        return
    }
    call.startNewTiming("polls.fetch", "Load basic poll data from database")
    val poll = Poll.fromID(pollID)
    if (poll == null) {
        call.respond(ReturnCode.INVALID_PARAMS)
        return
    }
    call.startNewTiming("poll.transform", "Transform poll data to detailed format")
    val detailedPoll = poll.asDetailedPoll()
    call.respond(detailedPoll)
}