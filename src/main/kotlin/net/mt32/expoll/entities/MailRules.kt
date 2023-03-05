package net.mt32.expoll.entities

import kotlinx.serialization.Serializable

@Serializable
data class MailRule(
    val id: String,
    val regex: String,
    val blacklist: Boolean
)