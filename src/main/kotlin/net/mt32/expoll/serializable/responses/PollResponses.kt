package net.mt32.expoll.serializable.responses

import kotlinx.serialization.Serializable
import net.mt32.expoll.entities.ISimpleUser
import net.mt32.expoll.entities.Poll
import net.mt32.expoll.tClientDate
import net.mt32.expoll.tClientDateTime
import net.mt32.expoll.tUserID

@Serializable
data class PollSimpleUser(
    override val firstName: String,
    override val lastName: String,
    override val username: String,
    override val id: String,
    val joinedTimestamp: tClientDateTime
): ISimpleUser

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
    val shareURL: String
)

@Serializable
data class UserVote(
    val user: PollSimpleUser,
    val votes: List<SimpleVote>
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
data class SimpleVote(
    val optionID: Int,
    val votedFor: Int?,
)

fun List<Poll>.asSummaryList(): List<PollSummary> {
    return sortedBy {
        it.updatedTimestamp.secondsSince1970
    }
        .reversed()
        .map { poll ->
            poll.asSimplePoll()
        }
}

fun List<Poll>.asPollListResponse(): PollListResponse {
    return PollListResponse(
        asSummaryList()
    )
}

@Serializable
data class PollListResponse(
    val polls: List<PollSummary>,
)

@Serializable
data class PollSummary(
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