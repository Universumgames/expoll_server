package net.mt32.expoll.notification

import kotlinx.serialization.Serializable
import net.mt32.expoll.commons.helper.UnixTimestamp
import net.mt32.expoll.commons.tClientDateTime


data class UniversalNotification(
    val title: String,
    val body: String,
    val titleArgs: List<String> = listOf(),
    val bodyArgs: List<String> = listOf(),
    val timestamp: UnixTimestamp = UnixTimestamp.now(),
    val expiration: UnixTimestamp = UnixTimestamp.now().addHours(6),
    val additionalData: Map<String, String> = mapOf()
) {
    @Serializable
    data class SerializableNotification(
        val title: String,
        val body: String,
        val titleArgs: List<String> = listOf(),
        val bodyArgs: List<String> = listOf(),
        val timestamp: tClientDateTime = 0,
        val expiration: tClientDateTime = 0,
        val additionalData: Map<String, String> = mapOf()
    )

    fun asSerializableNotification(): SerializableNotification {
        return SerializableNotification(title, body, titleArgs = titleArgs, bodyArgs = bodyArgs, timestamp = timestamp.toClient(), expiration = expiration.toClient(), additionalData = additionalData)
    }
}


interface NotificationHandler<T> {
    fun sendNotification(notification: UniversalNotification, device: T)
}