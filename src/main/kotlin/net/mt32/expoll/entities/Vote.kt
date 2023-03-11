package net.mt32.expoll.entities

import net.mt32.expoll.VoteValue
import net.mt32.expoll.database.DatabaseEntity
import net.mt32.expoll.database.UUIDLength
import net.mt32.expoll.helper.upsert
import net.mt32.expoll.tOptionID
import net.mt32.expoll.tPollID
import net.mt32.expoll.tUserID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class Vote : DatabaseEntity {
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

    override fun save(): Boolean {
        transaction {
            Vote.upsert(Vote.id) {
                it[id] = this@Vote.id
                it[userID] = this@Vote.userID
                it[pollID] = this@Vote.pollID
                it[optionID] = this@Vote.optionID
                it[votedFor] = this@Vote.votedFor.id
            }
        }
        return true
    }

    override fun toString(): String {
        return "Vote(id=$id, userID='$userID', pollID='$pollID', optionID=$optionID, votedFor=$votedFor)"
    }

    override fun delete(): Boolean {
        transaction {
            Vote.deleteWhere {
                (Vote.userID eq this@Vote.userID) and
                        (Vote.pollID eq this@Vote.pollID) and
                        (Vote.optionID eq this@Vote.optionID)
            }
        }
        return true
    }

    companion object : Table("vote") {
        val id = integer("id").autoIncrement()
        val userID = varchar("userId", UUIDLength)
        val pollID = varchar("pollId", UUIDLength)
        val optionID = integer("optionId")
        val votedFor = integer("votedFor")

        override val primaryKey = PrimaryKey(id)

        fun fromID(id: Int): Vote? {
            return transaction {
                val vote = Vote.select { Vote.id eq id }.firstOrNull()
                return@transaction vote?.let { Vote(it) }
            }
        }

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

        fun fromUserPollOption(userID: tUserID, pollID: tPollID, optionID: tOptionID): Vote? {
            return transaction {
                val result =
                    Vote.select {
                        (Vote.userID eq userID) and
                                (Vote.pollID eq pollID) and
                                (Vote.optionID eq optionID)
                    }
                        .firstOrNull()
                return@transaction result?.let { Vote(it) }
            }
        }

        fun fromUserPoll(userID: tUserID, pollID: tPollID): List<Vote> {
            return transaction {
                val result =
                    Vote.select {
                        (Vote.userID eq userID) and
                                (Vote.pollID eq pollID)
                    }
                return@transaction result.map { Vote(it) }
            }
        }

        private fun newID(): Int {
            return transaction {
                var id = 0
                do {
                    id++
                } while (fromID(id) != null)
                return@transaction id
            }
        }

        fun setVote(userID: tUserID, pollID: tPollID, optionID: tOptionID, votedFor: VoteValue): Vote {
            return transaction {
                val vote = fromUserPollOption(userID, pollID, optionID) ?: Vote(
                    newID(),
                    userID,
                    pollID,
                    optionID,
                    votedFor
                )
                vote.votedFor = votedFor
                vote.save()
                return@transaction vote
            }
        }
    }
}