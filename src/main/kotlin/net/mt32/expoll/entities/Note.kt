package net.mt32.expoll.entities

import kotlinx.serialization.Serializable
import net.mt32.expoll.database.DatabaseEntity
import net.mt32.expoll.database.UUIDLength
import net.mt32.expoll.helper.upsert
import net.mt32.expoll.tPollID
import net.mt32.expoll.tUserID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction


@Serializable
class PollUserNote : DatabaseEntity {
    val id: Int
    val userID: tUserID
    val pollID: tPollID
    var note: String

    constructor(id: Int, userID: tUserID, pollID: tPollID, note: String) {
        this.id = id
        this.userID = userID
        this.pollID = pollID
        this.note = note
    }

    private constructor(noteRow: ResultRow) {
        this.id = noteRow[PollUserNote.id]
        this.userID = noteRow[PollUserNote.userID]
        this.pollID = noteRow[PollUserNote.pollID]
        this.note = noteRow[PollUserNote.note]
    }

    override fun save(): Boolean {
        transaction {
            PollUserNote.upsert(PollUserNote.id) {
                it[id] = this@PollUserNote.id
                it[userID] = this@PollUserNote.userID
                it[pollID] = this@PollUserNote.pollID
                it[note] = this@PollUserNote.note
            }
        }
        return true
    }

    companion object : Table("poll_user_note") {
        val id = integer("id").autoIncrement()
        val userID = varchar("userId", UUIDLength)
        val pollID = varchar("pollId", UUIDLength)
        val note = varchar("note", 255)

        override val primaryKey = PrimaryKey(id)

        fun forUserAndPoll(userID: tUserID, pollID: tPollID): PollUserNote? {
            return transaction {
                val noteRow =
                    PollUserNote.select { (PollUserNote.userID eq userID) and (PollUserNote.pollID eq pollID) }
                        .firstOrNull()
                return@transaction noteRow?.let { PollUserNote(it) }
            }
        }

        fun forPoll(pollID: tPollID): List<PollUserNote> {
            return transaction {
                val noteRow =
                    PollUserNote.select { (PollUserNote.userID eq userID) and (PollUserNote.pollID eq pollID) }
                return@transaction noteRow.map { PollUserNote(it) }
            }
        }

        fun forUser(userID: tUserID): List<PollUserNote> {
            return transaction {
                val noteRow =
                    PollUserNote.select { PollUserNote.userID eq userID }
                return@transaction noteRow.map { PollUserNote(it) }
            }
        }
    }
}