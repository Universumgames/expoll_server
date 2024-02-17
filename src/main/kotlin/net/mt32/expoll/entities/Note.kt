package net.mt32.expoll.entities

import kotlinx.serialization.Serializable
import net.mt32.expoll.database.DatabaseEntity
import net.mt32.expoll.database.UUIDLength
import net.mt32.expoll.helper.upsertCustom
import net.mt32.expoll.tPollID
import net.mt32.expoll.tUserID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction


@Serializable
class PollUserNote : DatabaseEntity {
    val userID: tUserID
    val pollID: tPollID
    var note: String

    constructor(userID: tUserID, pollID: tPollID, note: String) {
        this.userID = userID
        this.pollID = pollID
        this.note = note
    }

    private constructor(noteRow: ResultRow) {
        this.userID = noteRow[PollUserNote.userID]
        this.pollID = noteRow[PollUserNote.pollID]
        this.note = noteRow[PollUserNote.note]
    }

    override fun save(): Boolean {
        transaction {
            if(note.isEmpty()){
                delete()
                return@transaction
            }
            PollUserNote.upsertCustom(PollUserNote.userID, PollUserNote.pollID) {
                it[userID] = this@PollUserNote.userID
                it[pollID] = this@PollUserNote.pollID
                it[note] = this@PollUserNote.note
            }
        }
        return true
    }

    override fun delete(): Boolean {
        transaction {
            PollUserNote.deleteWhere {
                (PollUserNote.userID eq this@PollUserNote.userID) and
                        (PollUserNote.pollID eq this@PollUserNote.pollID)
            }
        }
        return true
    }

    companion object : Table("poll_user_note") {
        val userID = varchar("userId", UUIDLength)
        val pollID = varchar("pollId", UUIDLength)
        val note = varchar("note", 255)

        override val primaryKey = PrimaryKey(userID, pollID)

        fun forUserAndPoll(userID: tUserID, pollID: tPollID): PollUserNote? {
            return transaction {
                val noteRow =
                    PollUserNote.selectAll().where { (PollUserNote.userID eq userID) and (PollUserNote.pollID eq pollID) }
                        .firstOrNull()
                return@transaction noteRow?.let { PollUserNote(it) }
            }
        }

        fun forPoll(pollID: tPollID): List<PollUserNote> {
            return transaction {
                val noteRow =
                    PollUserNote.selectAll().where { (PollUserNote.userID eq userID) and (PollUserNote.pollID eq pollID) }
                return@transaction noteRow.map { PollUserNote(it) }
            }
        }

        fun forUser(userID: tUserID): List<PollUserNote> {
            return transaction {
                val noteRow =
                    PollUserNote.selectAll().where { PollUserNote.userID eq userID }
                return@transaction noteRow.map { PollUserNote(it) }
            }
        }
    }
}