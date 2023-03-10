package net.mt32.expoll.serializable.responses

import kotlinx.serialization.Serializable
import net.mt32.expoll.entities.Poll
import net.mt32.expoll.tUserID

@Serializable
data class DetailedPollResponse(
    val pollID: String,
    val name: String,
    val admin: SimpleUser,
    val description: String,
    val maxPerUserVoteCount: Int,
    val userCount: Int,
    val lastUpdate: String,
    val created: String,
    val type: Int,
    val options: List<ComplexOption>,
    val userVotes: List<UserVote>,
    val userNotes: List<UserNote>,
    val allowsMaybe: Boolean,
    val allowsEditing: Boolean,
    val shareURL: String,
)

@Serializable
data class UserVote(
    val user: SimpleUser,
    val votes: List<PollVote>
)

@Serializable
data class UserNote(
    val userID: tUserID,
    val note: String?
)

@Serializable
data class ComplexOption(
    val id: Int,
    val value: String? = null,
    val dateStart: String? = null,
    val dateEnd: String? = null,
    val dateTimeStamp: String? = null,
    val dateTimeEnd: String? = null,
)

@Serializable
data class PollVote(
    val optionID: Int,
    val votedFor: Int?,
)

fun List<Poll>.asSimpleList(): List<SimplePoll> {
    return sortedBy {
        it.updatedTimestamp.toLong()
    }
        .reversed()
        .map { poll ->
            poll.asSimplePoll()
        }
}

fun List<Poll>.asPollListResponse(): PollListResponse {
    return PollListResponse(
        asSimpleList()
    )
}

@Serializable
data class PollListResponse(
    val polls: List<SimplePoll>,
)

@Serializable
data class SimplePoll(
    val pollID: String,
    val name: String,
    val admin: SimpleUser,
    val description: String,
    val userCount: Int,
    val lastUpdated: String,
    val type: Int,
    val editable: Boolean,
)
