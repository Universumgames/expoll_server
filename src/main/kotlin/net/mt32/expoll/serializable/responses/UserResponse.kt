package net.mt32.expoll.serializable.responses

import kotlinx.serialization.Serializable
import net.mt32.expoll.entities.PollUserNote
import net.mt32.expoll.serializable.request.VoteChange
import net.mt32.expoll.tClientDateTime
import net.mt32.expoll.tUserID

@Serializable
data class UserDataResponse(
    val id: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    val mail: String,
    val active: Boolean,
    val admin: Boolean,
    val createdTimestamp: tClientDateTime
)

@Serializable
data class CreateUserResponse(
    val jwt: String
)

@Serializable
data class SimpleUser(
    val firstName: String,
    val lastName: String,
    val username: String,
    val id: String,
)

@Serializable
data class UserPersonalizeResponse(
    val id: tUserID,
    var username: String,
    var firstName: String,
    var lastName: String,
    var mail: String,
    var polls: List<SimplePoll>,
    val votes: List<VoteChange>,
    var sessions: List<SafeSession>,
    var notes: List<PollUserNote>,
    var active: Boolean,
    var admin: Boolean,
    var superAdmin: Boolean,
    var authenticators: List<SimpleAuthenticator>,
)

@Serializable
data class SafeSession(
    val expiration: tClientDateTime,
    val userAgent: String?,
    val nonce: String,
    val active: Boolean
)