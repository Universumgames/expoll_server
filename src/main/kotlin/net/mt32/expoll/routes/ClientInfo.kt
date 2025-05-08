package net.mt32.expoll.routes

import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.mt32.expoll.commons.helper.ReturnCode
import net.mt32.expoll.commons.serializable.ClientInfo
import net.mt32.expoll.helper.checkVersionCompatibility

fun Route.clientCompatibilityRoute(){
    post("compatibility") {
        val clientVersion = call.receive<ClientInfo>()
        val compatible = checkVersionCompatibility(clientVersion)
        if (compatible) call.respond(ReturnCode.OK)
        else call.respond(ReturnCode.CONFLICT)
    }
}