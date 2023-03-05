package net.mt32.expoll.entities

import net.mt32.expoll.PollType


data class Poll(
    val admin: User,
    val id: String,
    var name: String,
    val createdTimestamp: Long,
    var updatedTimestamp: Long,
    var description: String,
    val type: PollType,
    var votes: MutableList<Vote>,
    var notes: MutableList<PollUserNote>,
    var maxPerUserVoteCount: Int,
    var allowsMaybe: Boolean,
    var allowsEditing: Boolean
){
}