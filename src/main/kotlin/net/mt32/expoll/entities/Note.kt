package net.mt32.expoll.entities

import kotlinx.serialization.Serializable
import net.mt32.expoll.tPollID


data class PollUserNote(
    val id: Int,
    val user: User,
    val pollID: tPollID,
    var note: String
)