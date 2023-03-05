package net.mt32.expoll.entities


data class APNDevice(
    val deviceID: String,
    val user: User,
    val creationTimestamp: Long
)