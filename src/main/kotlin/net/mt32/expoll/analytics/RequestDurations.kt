package net.mt32.expoll.analytics

import io.ktor.util.*
import kotlinx.serialization.Serializable

data class RequestDurationKey(
    val request: String,
    val method: String,
)

@Serializable
data class RequestDurationData(
    val request: String,
    val method: String,
    val durations: RequestDuration,
)

@Serializable
data class RequestDuration(
    val avgMs: Double,
    val minMs: Long,
    val maxMs: Long,
    val count: Long,
)

fun AnalyticsStorage.registerRequestDuration(request: String, method: String, duration: Long) {
    val key = RequestDurationKey(request, method)
    val durations = requestResponseDurations.getOrPut(key) { mutableListOf() }
    durations.add(duration)
    if(durations.size > 300){
        durations.removeAt(0)
    }
    requestResponseDurations[key] = durations
}

fun AnalyticsStorage.requestDurationsToResponse(): List<RequestDurationData> {
    return requestResponseDurations.map { (key, value) ->
        val avg = value.average()
        RequestDurationData(
            key.request.toLowerCasePreservingASCIIRules(),
            key.method.toUpperCasePreservingASCIIRules(),
            RequestDuration(
                avg,
                value.minOrNull() ?: 0,
                value.maxOrNull() ?: 0,
                value.size.toLong()
            )
        )
    }
}
