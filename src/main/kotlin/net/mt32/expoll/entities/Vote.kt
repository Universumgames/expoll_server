package net.mt32.expoll.entities

import net.mt32.expoll.*
import net.mt32.expoll.helper.upsert
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


    constructor(voteRow: ResultRow) {
        this.id = voteRow[Votes.id]
        this.userID = voteRow[Votes.userID]
        this.pollID = voteRow[Votes.pollID]
        this.optionID = voteRow[Votes.optionID]
        this.votedFor = VoteValue.values()[voteRow[Votes.votedFor]]
    }

    constructor(id: Int, userID: tUserID, pollID: tPollID, optionID: tOptionID, votedFor: VoteValue) {
        this.id = id
        this.userID = userID
        this.pollID = pollID
        this.optionID = optionID
        this.votedFor = votedFor
    }

    override fun save(){
        transaction {
            Votes.upsert (Votes.id) {
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

    companion object {
        fun fromUser(user: User): List<Vote> {
            return transaction {
                val result = Votes.select { Votes.userID eq user.id }
                return@transaction result.map { Vote(it) }
            }
        }

        fun fromPoll(poll: Poll): List<Vote> {
            return transaction {
                val result = Votes.select { Votes.pollID eq poll.id }
                return@transaction result.map { Vote(it) }
            }
        }
    }


}

object Votes : Table("vote") {
    val id = integer("id").autoIncrement()
    val userID = varchar("userId", UUIDLength)
    val pollID = varchar("pollId", UUIDLength)
    val optionID = integer("optionId")
    val votedFor = integer("votedFor")

    override val primaryKey = PrimaryKey(id)
}