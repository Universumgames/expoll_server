package net.mt32.expoll.serializable.responses

import kotlinx.serialization.Serializable

@Serializable
data class PollReceiveResponse(
    val polls: List<SimplePoll>,
)

@Serializable
data class SimplePoll(
    val pollId: String,
    val name: String,
    val admin: SimpleUser,
    val description: String,
    val userCount: Int,
    val lastUpdated: String,
    val type: Int,
    val editable: Boolean,
)

