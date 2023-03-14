package net.mt32.expoll.helper

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.util.*


private class ServerTimings : Principal {
    data class Timing(
        val key: String,
        val description: String,
        val startTime: UnixTimestamp,
        var endTime: UnixTimestamp? = null
    ) {
        override fun toString(): String {
            val endTime = this.endTime ?: return ""
            val duration = (endTime - startTime).asMillisSince1970()
            return key + ";desc=\"" + description + "\";dur=" + duration
        }
    }

    val timings: MutableList<Timing> = mutableListOf()

    var latestTiming: Timing? = null

    constructor() {
    }

    constructor(key: String, description: String) {
        startNewTiming(key, description)
    }

    fun startNewTiming(key: String, description: String) {
        val timing = latestTiming
        if (timing != null) {
            timing.endTime = UnixTimestamp.now()
            timings.add(timing)
        }
        latestTiming = Timing(key, description, UnixTimestamp.now())
    }

    fun finishTiming() {
        startNewTiming("", "")
        latestTiming = null
    }
}

fun ApplicationCall.startNewTiming(key: String, description: String){
    val timings = this.attributes[timingsKey]
    timings.startNewTiming(key, description)
}

private val timingsKey = AttributeKey<ServerTimings>("timings")

var ServerTimingsHeader = createApplicationPlugin("ServerTimings"){

    onCall { call->
        call.attributes.put(timingsKey, ServerTimings("request.receive", "Receive request and prepare response"))
    }

    onCallRespond { call->
        val timing = call.attributes[timingsKey]
        timing.finishTiming()
        call.response.header(
            "Server-Timing",
            timing.timings.sortedBy { it.startTime.millisSince1970 }.map { it.toString() }.joinToString(",")
        )
    }
}