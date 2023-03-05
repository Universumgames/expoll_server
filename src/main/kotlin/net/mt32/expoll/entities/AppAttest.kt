package net.mt32.expoll.entities


data class AppAttest(
    val uuid: String,
    val challenge: String,
    val createdAtTimestamp: Long
)