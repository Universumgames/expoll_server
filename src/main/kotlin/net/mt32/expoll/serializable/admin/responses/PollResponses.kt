package net.mt32.expoll.serializable.admin.responses

import kotlinx.serialization.Serializable
import net.mt32.expoll.serializable.responses.PollSummary

@Serializable
data class AdminPollResponse(
    val polls: List<PollSummary>,
    val totalCount: Int
)