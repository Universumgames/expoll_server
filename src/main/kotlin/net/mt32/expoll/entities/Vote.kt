package net.mt32.expoll.entities

import net.mt32.expoll.VoteValue
import net.mt32.expoll.database.DatabaseEntity
import net.mt32.expoll.database.UUIDLength
import net.mt32.expoll.helper.upsert
import net.mt32.expoll.tOptionID
import net.mt32.expoll.tPollID
import net.mt32.expoll.tUserID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class Vote: DatabaseEntity {
    val id: Int
    val userID: tUserID
    val pollID: tPollID
    val optionID: tOptionID
    var votedFor: VoteValue

    constructor(id: Int, userID: tUserID, pollID: tPollID, optionID: tOptionID, votedFor: VoteValue) {
        this.id = id
        this.userID = userID
        this.pollID = pollID
        this.optionID = optionID
        this.votedFor = votedFor
    }

    private constructor(voteRow: ResultRow) {
        this.id = voteRow[Vote.id]
        this.userID = voteRow[Vote.userID]
        this.pollID = voteRow[Vote.pollID]
        this.optionID = voteRow[Vote.optionID]
        this.votedFor = VoteValue.values()[voteRow[Vote.votedFor]]
    }

    override fun save(){
        transaction {
            Vote.upsert (Vote.id) {
                it[id] = this@Vote.id
                it[userID] = this@Vote.userID
                it[pollID]= this@Vote.pollID
                it[optionID] = this@Vote.optionID
                it[votedFor] = this@Vote.votedFor.id
            }
        }
    }

    override fun toString(): String {
        return "Vote(id=$id, userID='$userID', pollID='$pollID', optionID=$optionID, votedFor=$votedFor)"
    }

    companion object : Table("vote"){

        val id = integer("id").autoIncrement()
        val userID = varchar("userId", UUIDLength)
        val pollID = varchar("pollId", UUIDLength)
        val optionID = integer("optionId")
        val votedFor = integer("votedFor")

        override val primaryKey = PrimaryKey(id)
        fun fromUser(user: User): List<Vote> {
            return transaction {
                val result = Vote.select { Vote.userID eq user.id }
                return@transaction result.map { Vote(it) }
            }
        }

        fun fromPoll(poll: Poll): List<Vote> {
            return transaction {
                val result = Vote.select { Vote.pollID eq poll.id }
                return@transaction result.map { Vote(it) }
            }
        }

        fun fromPoll(pollID: tPollID): List<Vote> {
            return transaction {
                val result = Vote.select { Vote.pollID eq pollID }
                return@transaction result.map { Vote(it) }
            }
        }
    }
}