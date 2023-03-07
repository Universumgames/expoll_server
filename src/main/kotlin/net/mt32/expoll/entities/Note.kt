package net.mt32.expoll.entities

import net.mt32.expoll.database.DatabaseEntity
import net.mt32.expoll.database.UUIDLength
import net.mt32.expoll.tPollID
import net.mt32.expoll.tUserID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction


class PollUserNote : DatabaseEntity {
    val id: Int
    val userID: tUserID
    val pollID: tPollID
    var note: String

    constructor(id: Int, userID: tUserID, pollID: tPollID, note: String) : super() {
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

    override fun save() {
        TODO("Not yet implemented")
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
    }
}