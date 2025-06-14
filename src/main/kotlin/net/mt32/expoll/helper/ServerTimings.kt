package net.mt32.expoll.helper

import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.auth.*
import io.ktor.util.*
import net.mt32.expoll.commons.helper.UnixTimestamp


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
            return "$key;desc=\"$description\";dur=$duration"
        }
    }

    val timings: MutableList<Timing> = mutableListOf()

    var latestTiming: Timing? = null

    constructor(key: String, description: String) {
        startNewTiming(key, description)
    }

    fun startNewTiming(key: String, description: String) {
        val timing = latestTiming
        if (timing != null) {
            timing.endTime = UnixTimestamp.Companion.now()
            timings.add(timing)
        }
        latestTiming = Timing(key, description, UnixTimestamp.Companion.now())
    }

    fun finishTiming() {
        startNewTiming("", "")
        latestTiming = null
    }
}

fun ApplicationCall.startNewTiming(key: String, description: String) {
    val timings = this.attributes[timingsKey]
    timings.startNewTiming(key, description)
}

private val timingsKey = AttributeKey<ServerTimings>("timings")


var ServerTimingsHeader = createApplicationPlugin("ServerTimings") {

    on(CallSetup) { call ->
        call.attributes.put(timingsKey, ServerTimings("request.receive", "Receive request and prepare response"))
    }

    onCallRespond { call ->
        val timing = call.attributes[timingsKey]
        timing.finishTiming()
        try {
            call.response.headers.append(
                "Server-Timing",
                timing.timings.sortedBy { it.startTime.millisSince1970 }.joinToString(",") { it.toString() }
            )
        } catch (e: UnsupportedOperationException) {
            // ignore error "Headers can no longer be set because response was already completed"
        }
    }
}