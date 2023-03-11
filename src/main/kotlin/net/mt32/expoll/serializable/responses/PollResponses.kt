package net.mt32.expoll.serializable.responses

import kotlinx.serialization.Serializable
import net.mt32.expoll.entities.Poll
import net.mt32.expoll.tClientDate
import net.mt32.expoll.tClientDateTime
import net.mt32.expoll.tUserID

@Serializable
data class DetailedPollResponse(
    val pollID: String,
    val name: String,
    val admin: SimpleUser,
    val description: String,
    val maxPerUserVoteCount: Int,
    val userCount: Int,
    val lastUpdated: tClientDateTime,
    val created: tClientDateTime,
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
    val id: Int? = null,
    val value: String? = null,
    val dateStart: tClientDate? = null,
    val dateEnd: tClientDate? = null,
    val dateTimeStart: tClientDateTime? = null,
    val dateTimeEnd: tClientDateTime? = null,
)

// TODO duplicate of VoteChange?
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
    val lastUpdated: tClientDateTime,
    val type: Int,
    val editable: Boolean,
)

@Serializable
data class PollCreatedResponse(
    val pollID: String
)