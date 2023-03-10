package net.mt32.expoll.entities

import net.mt32.expoll.PollType
import net.mt32.expoll.config
import net.mt32.expoll.database.DatabaseEntity
import net.mt32.expoll.database.UUIDLength
import net.mt32.expoll.helper.UnixTimestamp
import net.mt32.expoll.helper.toUnixTimestamp
import net.mt32.expoll.helper.upsert
import net.mt32.expoll.serializable.responses.*
import net.mt32.expoll.tPollID
import net.mt32.expoll.tUserID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

interface IPoll {
    val admin: User
    val id: String
    var name: String
    val createdTimestamp: UnixTimestamp
    var updatedTimestamp: UnixTimestamp
    var description: String
    val type: PollType
    val votes: List<Vote>
    val notes: List<PollUserNote>
    var maxPerUserVoteCount: Int
    var allowsMaybe: Boolean
    var allowsEditing: Boolean
}

class Poll : DatabaseEntity, IPoll {

    override val admin: User
        get() {
            return User.loadFromID(adminID)!!
        }
    val adminID: tUserID
    override val id: String
    override var name: String
    override val createdTimestamp: UnixTimestamp
    override var updatedTimestamp: UnixTimestamp
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
    val users: List<User>
        get() {
            return usersInPoll(id)
        }
    override var maxPerUserVoteCount: Int
    override var allowsMaybe: Boolean
    override var allowsEditing: Boolean

    val options: List<PollOption>
        get() {
            return when (type) {
                PollType.STRING -> PollOptionString.fromPollID(id)
                PollType.DATE -> PollOptionDate.fromPollID(id)
                PollType.DATETIME -> PollOptionDateTime.fromPollID(id)
            }
        }

    constructor(
        adminID: tUserID,
        id: String,
        name: String,
        createdTimestamp: UnixTimestamp,
        updatedTimestamp: UnixTimestamp,
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


    private constructor(pollRow: ResultRow) {
        this.id = pollRow[Poll.id]
        this.adminID = pollRow[Poll.adminID]
        this.name = pollRow[Poll.name]
        this.description = pollRow[Poll.description]
        this.type = PollType.valueOf(pollRow[Poll.type])
        this.createdTimestamp = pollRow[Poll.createdTimestamp].toUnixTimestamp()
        this.updatedTimestamp = pollRow[Poll.createdTimestamp].toUnixTimestamp()
        this.maxPerUserVoteCount = pollRow[Poll.maxPerUserVoteCount]
        this.allowsMaybe = pollRow[Poll.allowsMaybe]
        this.allowsEditing = pollRow[Poll.allowsEditing]
    }

    override fun save(): Boolean {
        transaction {
            options.forEach { option ->
                option.save()
            }
            notes.forEach { note ->
                note.save()
            }
            votes.forEach { vote ->
                vote.save()
            }
            transaction {
                Poll.upsert(Poll.id) {
                    it[Poll.id] = this@Poll.id
                    it[Poll.adminID] = this@Poll.adminID
                    it[Poll.name] = this@Poll.name
                    it[Poll.description] = this@Poll.description
                    it[Poll.type] = this@Poll.type.id
                    it[Poll.createdTimestamp] = this@Poll.createdTimestamp.toLong()
                    it[Poll.createdTimestamp] = this@Poll.updatedTimestamp.toLong()
                    it[Poll.maxPerUserVoteCount] = this@Poll.maxPerUserVoteCount
                    it[Poll.allowsMaybe] = this@Poll.allowsMaybe
                    it[Poll.allowsEditing] = this@Poll.allowsEditing
                }
            }
        }
        return true
    }

    companion object : Table("poll") {
        const val MAX_DESCRIPTION_LENGTH = 65535

        val id = varchar("id", UUIDLength)
        val adminID = varchar("adminId", UUIDLength)
        val name = varchar("name", 255)
        val createdTimestamp = long("createdTimestamp")
        val updatedTimestamp = long("updatedTimestamp")
        val description = varchar("description", MAX_DESCRIPTION_LENGTH)
        val type = integer("type")
        val maxPerUserVoteCount = integer("maxPerUserVoteCount")
        val allowsMaybe = bool("allowsMaybe")
        val allowsEditing = bool("allowsEditing")

        override val primaryKey = PrimaryKey(id)

        fun fromID(pollID: tPollID): Poll? {
            return transaction {
                val pollRow = Poll.select { Poll.id eq pollID }.firstOrNull()
                return@transaction pollRow?.let { Poll(it) }
            }
        }

        fun accessibleForUser(userID: tUserID): List<Poll> {
            return transaction {
                val pollIDs = UserPolls.select { UserPolls.userID eq userID }.map { it[UserPolls.pollID] }
                return@transaction pollIDs.map { Poll.fromID(it) }.requireNoNulls()
            }
        }

        fun usersInPoll(pollID: tPollID): List<User> {
            return transaction {
                val userIDs = UserPolls.select { UserPolls.pollID eq pollID }.map { it[UserPolls.userID] }
                return@transaction userIDs.map { User.loadFromID(it) }.requireNoNulls()
            }
        }
    }

    fun asDetailedPoll(): DetailedPollResponse {
        return DetailedPollResponse(
            id,
            name,
            admin.asSimpleUser(),
            description,
            maxPerUserVoteCount,
            users.size,
            updatedTimestamp.toJSDate(),
            createdTimestamp.toJSDate(),
            type.id,
            options.map { it.toComplexOption() },
            users.map { user ->
                val votes = Vote.fromUserPoll(user.id, id)
                val existingVotesOptionIds = votes.map { it.optionID }
                val missingVotes = options.map { it.id }.filterNot { existingVotesOptionIds.contains(it) }
                UserVote(
                    user.asSimpleUser(),
                    votes.map { note -> PollVote(note.optionID, note.votedFor.id) } +
                            // add null votes for non existing votes on options
                            missingVotes.map {
                                PollVote(
                                    it,
                                    null
                                )
                            })
            },
            users.map { user ->
                val note = notes.find { note -> note.userID == user.id }
                UserNote(user.id, note?.note)
            }.filterNot { it.note == null },
            allowsMaybe,
            allowsEditing,
            config.shareURLPrefix + id
        )
    }

    fun asSimplePoll(): SimplePoll {
        return SimplePoll(
            id,
            name,
            admin.asSimpleUser(),
            description,
            users.size,
            updatedTimestamp.toJSDate(),
            type.id,
            allowsEditing
        )
    }
}

object UserPolls : Table("user_polls_poll") {
    val userID = varchar("userId", UUIDLength)
    val pollID = varchar("pollId", UUIDLength)

    override val primaryKey = PrimaryKey(userID, pollID)
}