package net.mt32.expoll.helper

import io.ktor.server.application.*
import io.ktor.server.response.*


class ServerTimings {
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

    constructor(){
    }

    constructor(key: String, description: String){
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

fun ApplicationCall.addServerTiming(timing: ServerTimings) {
    timing.finishTiming()
    this.response.header("Server-Timing", timing.timings.sortedBy { it.startTime.millisSince1970 }.map { it.toString() }.joinToString(","))
}