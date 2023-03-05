package net.mt32.expoll.entities

data class DeleteConfirmation(
    val id: String,
    val user: User,
    val expirationTimestamp: Long
)