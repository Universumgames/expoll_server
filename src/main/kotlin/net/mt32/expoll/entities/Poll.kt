package net.mt32.expoll.entities

import net.mt32.expoll.PollType
import net.mt32.expoll.database.DatabaseEntity
import net.mt32.expoll.database.UUIDLength
import net.mt32.expoll.tUserID
import org.jetbrains.exposed.sql.Table

interface IPoll{
    val admin: User
    val id: String
    var name: String
    val createdTimestamp: Long
    var updatedTimestamp: Long
    var description: String
    val type: PollType
    val votes: List<Vote>
    val notes: List<PollUserNote>
    var maxPerUserVoteCount: Int
    var allowsMaybe: Boolean
    var allowsEditing: Boolean
}

class Poll: DatabaseEntity, IPoll{

    override val admin: User
        get(){
            return User.loadFromID(adminID)!!
        }
    val adminID: tUserID
    override val id: String
    override var name: String
    override val createdTimestamp: Long
    override var updatedTimestamp: Long
    override var description: String
    override val type: PollType
    override val votes: List<Vote>
        get() {
            return Vote.fromPoll(id)
        }
    override val notes: List<PollUserNote>
        get() {
            return PollUserNote.forPoll(id)
        }
    override var maxPerUserVoteCount: Int
    override var allowsMaybe: Boolean
    override var allowsEditing: Boolean

    constructor(
        adminID: tUserID,
        id: String,
        name: String,
        createdTimestamp: Long,
        updatedTimestamp: Long,
        description: String,
        type: PollType,
        maxPerUserVoteCount: Int,
        allowsMaybe: Boolean,
        allowsEditing: Boolean
    ) : super() {
        this.adminID = adminID
        this.id = id
        this.name = name
        this.createdTimestamp = createdTimestamp
        this.updatedTimestamp = updatedTimestamp
        this.description = description
        this.type = type
        this.maxPerUserVoteCount = maxPerUserVoteCount
        this.allowsMaybe = allowsMaybe
        this.allowsEditing = allowsEditing
    }


    override fun save() {
        TODO("Not yet implemented")
    }

    companion object: Table("poll"){


        fun accessibleForUser(userID: tUserID): List<Poll>{
            TODO()
        }
    }
}

object UserPolls: Table("user_polls_poll"){
    val userID = varchar("userId", UUIDLength)
    val pollID = varchar("pollId", UUIDLength)
}