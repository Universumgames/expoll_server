package net.mt32.expoll.analytics

import kotlinx.serialization.Serializable
import net.mt32.expoll.commons.helper.UnixTimestamp

data class RequestCountStorageElement(
    val name: String,
    var count: Long,
    var lastReset: UnixTimestamp
) {
    fun reset() {
        count = 0
        lastReset = UnixTimestamp.now()
    }

    fun toResponse(): RequestCount {
        val diffMilli = UnixTimestamp.now().millisSince1970 - lastReset.millisSince1970
        return RequestCount(name, count / diffMilli.toDouble() * 1000, count, lastReset.secondsSince1970)
    }
}

@Serializable
data class RequestCount(
    val name: String,
    val cps: Double,
    val countSinceReset: Long,
    val resetTimestampSec: Long
)

fun AnalyticsStorage.registerRequest(key: String) {
    requestCountStorage[key] =
        RequestCountStorageElement(key, (requestCountStorage[key]?.count ?: 0) + 1, requestCountStorage[key]?.lastReset ?: UnixTimestamp.now())
}