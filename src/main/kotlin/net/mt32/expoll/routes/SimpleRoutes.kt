package net.mt32.expoll.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.mt32.expoll.entities.MailRule
import net.mt32.expoll.entities.Poll
import net.mt32.expoll.commons.helper.ReturnCode
import net.mt32.expoll.serializable.responses.MailRegexRules

fun Route.simpleRoutes(){
    route("simple"){
        get("/mailregex"){
            getMailRegexRules(call)
        }
        get("/poll/{pollid}/title"){
            getPollTitle(call)
        }
    }
}

private suspend fun getPollTitle(call: ApplicationCall){
    val pollID = call.parameters["pollid"]
    if(pollID == null){
        call.respond(ReturnCode.BAD_REQUEST)
        return
    }
    val poll = Poll.fromID(pollID)
    if(poll == null){
        call.respond(ReturnCode.BAD_REQUEST)
        return
    }
    call.respond(poll.name)
}


private suspend fun getMailRegexRules(call:ApplicationCall){
    call.respond(MailRegexRules(MailRule.all()))
}