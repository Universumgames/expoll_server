package net.mt32.expoll.routes

import io.ktor.server.application.*
import io.ktor.server.routing.*
import net.mt32.expoll.tPollID

fun Route.pollRoutes(){
    route("/poll"){
//TODO add poll routes
    }
}

private suspend fun editPoll(call: ApplicationCall){
    TODO()
}

private suspend fun createPoll(call:ApplicationCall){
    TODO()
}

private suspend fun getPolls(call: ApplicationCall){
    TODO()
}

private suspend fun getPollList(call: ApplicationCall){
    TODO()
}

private suspend fun getDetailedPoll(call: ApplicationCall, pollID: tPollID){
    TODO()
}