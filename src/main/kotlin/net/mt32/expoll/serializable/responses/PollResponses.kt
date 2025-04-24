package net.mt32.expoll.serializable.responses

import kotlinx.serialization.Serializable
import net.mt32.expoll.entities.ISimpleUser
import net.mt32.expoll.entities.Poll
import net.mt32.expoll.entities.User
import net.mt32.expoll.commons.tClientDate
import net.mt32.expoll.commons.tClientDateTime
import net.mt32.expoll.commons.tOptionID

@Serializable
data class PollSimpleUser(
    override val firstName: String,
    override val lastName: String,
    override val username: String,
    override val id: String,
    val joinedTimestamp: tClientDateTime
): ISimpleUser

interface PollResponse {
    val pollID: String
    val name: String
    val admin: SimpleUser
    val description: String
    val userCount: Int
    val lastUpdated: tClientDateTime
    val type: Int
    val allowsEditing: Boolean
    val hidden: Boolean
}

@Serializable
data class DetailedPollResponse(
    override val pollID: String,
    override val name: String,
    override val admin: SimpleUser,
    override val description: String,
    val maxPerUserVoteCount: Int,
    override val userCount: Int,
    override val lastUpdated: tClientDateTime,
    val created: tClientDateTime,
    override val type: Int,
    val options: List<ComplexOption>,
    val mostRelevantOptionID: tOptionID?,
    val userVotes: List<UserVote>,
    val allowsMaybe: Boolean,
    override val allowsEditing: Boolean,
    val privateVoting: Boolean,
    val shareURL: String,
    override val hidden: Boolean,
    val defaultVote: Int?,
): PollResponse

@Serializable
data class UserVote(
    val user: PollSimpleUser,
    val note: String?,
    val votes: List<SimpleVote>
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

@Serializable
data class SimpleVote(
    val optionID: Int,
    val votedFor: Int
)

fun List<Poll>.asSummaryList(user: User?): List<PollSummary> {
    return sortedBy {
        it.updatedTimestamp.secondsSince1970
    }
        .reversed()
        .map { poll ->
            poll.asSimplePoll(user)
        }
}

fun List<Poll>.asPollListResponse(user: User?): PollListResponse {
    return PollListResponse(
        asSummaryList(user)
    )
}

@Serializable
data class PollListResponse(
    val polls: List<PollSummary>,
)

@Serializable
data class PollSummary(
    override val pollID: String,
    override val name: String,
    override val admin: SimpleUser,
    override val description: String,
    override val userCount: Int,
    override val lastUpdated: tClientDateTime,
    override val type: Int,
    override val allowsEditing: Boolean,
    override val hidden: Boolean
): PollResponse

@Serializable
data class PollCreatedResponse(
    val pollID: String
)