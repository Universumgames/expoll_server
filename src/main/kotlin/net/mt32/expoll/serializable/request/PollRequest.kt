package net.mt32.expoll.serializable.request

import kotlinx.serialization.Serializable
import net.mt32.expoll.entities.PollSearchParameters
import net.mt32.expoll.serializable.responses.ComplexOption
import net.mt32.expoll.serializable.responses.UserNote
import net.mt32.expoll.tPollID
import net.mt32.expoll.tUserID

@Serializable
data class CreatePollRequest(
    val name: String,
    val maxPerUserVoteCount: Int,
    val description: String,
    val type: Int,
    val options: List<ComplexOption>,
    val allowsMaybe: Boolean,
    val allowsEditing: Boolean,
)

@Serializable
data class EditPollRequest(
    val pollID: tPollID,
    val delete: Boolean? = null,
    val name: String? = null,
    val description: String? = null,
    val maxPerUserVoteCount: Int? = null,
    val allowsMaybe: Boolean? = null,
    val allowsEditing: Boolean? = null,
    val userRemove: List<tUserID> = listOf(),
    val userAdd: List<tUserID> = listOf(),
    val votes: List<VoteChange> = listOf(),
    val options: List<ComplexOption> = listOf(),
    val notes: List<UserNote> = listOf()
)

@Serializable
data class PollRequest(
    val pollID: tPollID? = null,
    val searchParameters: PollSearchParameters? = null,
)