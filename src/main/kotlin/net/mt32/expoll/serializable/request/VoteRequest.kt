package net.mt32.expoll.serializable.request

import kotlinx.serialization.Serializable
import net.mt32.expoll.commons.tOptionID
import net.mt32.expoll.commons.tPollID
import net.mt32.expoll.commons.tUserID

@Serializable
data class VoteChange(
    val pollID: tPollID,
    val optionID: tOptionID,
    val votedFor: Int,
    val userID: tUserID? = null
)