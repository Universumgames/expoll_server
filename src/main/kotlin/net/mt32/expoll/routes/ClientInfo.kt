package net.mt32.expoll.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import net.mt32.expoll.helper.ReturnCode
import net.mt32.expoll.helper.checkVersionCompatibility

@Serializable
data class ClientInfo(
    val version: String,
    val build: String? = null,
    val platform: String? = null,
)


fun Route.clientCompatibilityRoute(){
    // TODO remove this route, just for backwards compatibility
    options("compliance") {
        val body = call.receiveText()
        println(body)
        val clientVersion = call.receive<ClientInfo>()
        val compatible = checkVersionCompatibility(clientVersion)
        if (compatible) call.respond(ReturnCode.OK)
        else call.respond(ReturnCode.CONFLICT)
    }
    post("compatibility") {
        val clientVersion = call.receive<ClientInfo>()
        val compatible = checkVersionCompatibility(clientVersion)
        if (compatible) call.respond(ReturnCode.OK)
        else call.respond(ReturnCode.CONFLICT)
    }
}