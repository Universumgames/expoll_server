package net.mt32.expoll.serializable.request

import kotlinx.serialization.Serializable
import net.mt32.expoll.entities.PollUserNote
import net.mt32.expoll.serializable.responses.DetailedPollResponse
import net.mt32.expoll.serializable.responses.PollVote
import net.mt32.expoll.serializable.responses.SimpleAuthenticator
import net.mt32.expoll.tUserID


@Serializable
data class UserPersonalizeResponse(
    val id: tUserID,
    var username: String,
    var firstName: String,
    var lastName: String,
    var mail: String,
    var polls: List<DetailedPollResponse>,
    val votes: List<PollVote>,
    var session: List<SafeSession>,
    var notes: List<PollUserNote>,
    var active: Boolean,
    var admin: Boolean,
    var authenticators: List<SimpleAuthenticator>,
)

@Serializable
data class SafeSession(
    val expiration: String,
    val userAgent: String,
    val shortKey: String
)