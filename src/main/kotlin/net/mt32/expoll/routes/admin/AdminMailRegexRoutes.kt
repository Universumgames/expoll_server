package net.mt32.expoll.routes.admin

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.mt32.expoll.entities.MailRule
import net.mt32.expoll.commons.helper.ReturnCode
import net.mt32.expoll.serializable.admin.request.MailRegexEditRequest
import net.mt32.expoll.serializable.responses.MailRegexRules

internal fun Route.adminRegexRoutes() {
    route("/mailregex") {
        get {
            getMaiLRegexRules(call)
        }
        post {
            editMailRegexRules(call)
        }
    }
}

private suspend fun editMailRegexRules(call: ApplicationCall) {
    val rules: MailRegexEditRequest = call.receive()
    // clear all rules
    MailRule.all().forEach { it.delete() }
    // save new rules
    rules.mailRegex.map {
        MailRule(it.regex, it.blacklist)
    }.forEach { it.save() }

    call.respond(ReturnCode.OK)
}

private suspend fun getMaiLRegexRules(call: ApplicationCall) {
    call.respond(MailRegexRules(MailRule.all()))
}