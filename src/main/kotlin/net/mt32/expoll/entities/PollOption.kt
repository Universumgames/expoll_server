package net.mt32.expoll.entities

import kotlinx.serialization.Serializable
import net.mt32.expoll.tOptionID
import net.mt32.expoll.tPollID

interface PollOption {
    val pollID: tPollID
    val id: tOptionID
}

data class PollOptionString(
    val value: String,
    override val pollID: tPollID,
    override val id: tOptionID
) : PollOption

data class PollOptionDate(
    val dateStartTimestamp: Long,
    val dateEndTimestamp: Long?,
    override val pollID: tPollID,
    override val id: tOptionID
) : PollOption

data class PollOptionDateTime(
    val dateTimeStartTimestamp: Long,
    val dateTImeEndTimestamp: Long?,
    override val pollID: tPollID,
    override val id: tOptionID
) : PollOption