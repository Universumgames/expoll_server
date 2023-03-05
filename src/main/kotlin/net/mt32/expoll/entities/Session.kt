package net.mt32.expoll.entities


data class Session(
    val loginkey: String,
    val expirationTimestamp: Long,
    var userAgent: String,
    val user: User
)