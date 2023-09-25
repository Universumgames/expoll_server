package net.mt32.expoll.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import net.mt32.expoll.helper.ReturnCode
import net.mt32.expoll.helper.checkVersionCompatibility

@Serializable
data class Compliance(
    val version: String,
    val build: String? = null,
    val platform: String? = null,
)


fun Route.complianceRoute(){
    options("compliance") {
        val clientVersion = call.receive<Compliance>()
        val compatible = checkVersionCompatibility(clientVersion)
        if (compatible) call.respond(ReturnCode.OK)
        else call.respond(ReturnCode.CONFLICT)
    }
}