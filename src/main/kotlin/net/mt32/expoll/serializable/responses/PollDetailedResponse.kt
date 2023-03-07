package net.mt32.expoll.serializable.responses

import kotlinx.serialization.Serializable

@Serializable
data class PollDetailedResponse(
    val pollID: String,
    val name: String,
    val admin: SimpleUser,
    val description: String,
    val maxPerUserVoteCount: Int,
    val userCount: Int,
    val lastUpdate: String,
    val created: String,
    val type: Int,
    val options: PollOptions,
    val userVotes: Map<SimpleUser, PollVote>,
    val userNotes: Map<SimpleUser, String>,
    val allowsMaybe: Boolean,
    val allowsEditing: Boolean,
    val shareURL: String,
)

@Serializable
data class PollOptions(
    val optionId: Int,
    val value: String,
    val dateStart: String?,
    val dateEnd: String?,
    val dateTimeStamp: String?,
    val dateTimeEnd: String?,
)

@Serializable
data class PollVote(
    val optionId: Int,
    val votedFor: Int,
)