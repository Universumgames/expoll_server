package net.mt32.expoll.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.mt32.expoll.config
import net.mt32.expoll.entities.ServerInfo
import net.mt32.expoll.helper.compareVersion
import net.mt32.expoll.helper.getDataFromAny

fun Route.apiRouting(){
    route("/"){
        get("/test") {
            call.respondText("Hello World!")
        }
        get("compliance"){
            val clientVersion = getDataFromAny(call, "version")
            if(clientVersion == null){
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }
            call.respond(HttpStatusCode.OK,"${compareVersion(clientVersion, config.minimumRequiredClientVersion)}")
        }
        get("serverInfo"){
            call.respond(ServerInfo.instance)
        }
    }
}