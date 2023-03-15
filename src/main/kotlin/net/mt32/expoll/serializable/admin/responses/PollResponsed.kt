package net.mt32.expoll.serializable.admin.responses

import kotlinx.serialization.Serializable
import net.mt32.expoll.serializable.responses.SimplePoll

@Serializable
data class AdminPollResponse(
    val polls: List<SimplePoll>,
    val totalCount: Int
)