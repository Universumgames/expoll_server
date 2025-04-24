package net.mt32.expoll.serializable.responses

import kotlinx.serialization.Serializable
import net.mt32.expoll.entities.ISimpleUser
import net.mt32.expoll.entities.PollUserNote
import net.mt32.expoll.serializable.request.Platform
import net.mt32.expoll.serializable.request.VoteChange
import net.mt32.expoll.commons.tClientDateTime
import net.mt32.expoll.commons.tPollID
import net.mt32.expoll.commons.tUserID

interface IUserDataResponse{
    val id: String
    val username: String
    val firstName: String
    val lastName: String
    val mail: String
    val active: Boolean
    val admin: Boolean
    val createdTimestamp: tClientDateTime
    val pollsOwned: Long
    val maxPollsOwned: Long
}

@Serializable
data class UserDataResponse(
    override val id: String,
    override val username: String,
    override val firstName: String,
    override val lastName: String,
    override val mail: String,
    override val active: Boolean,
    override val admin: Boolean,
    override val createdTimestamp: tClientDateTime,
    override val pollsOwned: Long,
    override val maxPollsOwned: Long
): IUserDataResponse

@Serializable
data class CreateUserResponse(
    val jwt: String
)

@Serializable
data class SimpleUser(
    override val firstName: String,
    override val lastName: String,
    override val username: String,
    override val id: String,
): ISimpleUser

@Serializable
data class UserPersonalizeResponse(
    override val id: tUserID,
    override var username: String,
    override var firstName: String,
    override var lastName: String,
    override  var mail: String,
    var polls: List<StrippedPollData>,
    val votes: List<VoteChange>,
    var sessions: List<SafeSession>,
    var notes: List<PollUserNote>,
    override var active: Boolean,
    override var admin: Boolean,
    var superAdmin: Boolean,
    var authenticators: List<SimpleAuthenticator>,
    override val createdTimestamp: tClientDateTime,
    override val pollsOwned: Long,
    override val maxPollsOwned: Long
): IUserDataResponse

@Serializable
data class StrippedPollData(
    val pollID: tPollID
)

@Serializable
data class SafeSession(
    val expiration: tClientDateTime,
    val userAgent: String?,
    val platform: Platform,
    val version: String?,
    val nonce: String,
    val active: Boolean
)