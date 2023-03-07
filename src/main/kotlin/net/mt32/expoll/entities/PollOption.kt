package net.mt32.expoll.entities

import net.mt32.expoll.database.DatabaseEntity
import net.mt32.expoll.database.UUIDLength
import net.mt32.expoll.tOptionID
import net.mt32.expoll.tPollID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

interface PollOption {
    val pollID: tPollID
    val id: tOptionID
}

class PollOptionString : PollOption, DatabaseEntity {
    val value: String
    override val pollID: tPollID
    override val id: tOptionID

    constructor(value: String, pollID: tPollID, id: tOptionID) {
        this.value = value
        this.pollID = pollID
        this.id = id
    }

    private constructor(optionRow: ResultRow) {
        id = optionRow[PollOptionString.id]
        pollID = optionRow[PollOptionString.pollID]
        value = optionRow[PollOptionString.value]
    }

    override fun save() {
        TODO("Not yet implemented")
    }

    companion object : Table("poll_option_date_time") {
        val id = integer("id").autoIncrement()
        val value = varchar("value", 255)
        val pollID = varchar("pollId", UUIDLength)

        override val primaryKey = PrimaryKey(id)

        fun fromPollIDAndID(pollID: tPollID, optionID: tOptionID): PollOptionString? {
            return transaction {
                val result =
                    PollOptionString.select { (PollOptionString.pollID eq pollID) and (PollOptionString.id eq optionID) }
                        .firstOrNull()
                return@transaction result?.let { PollOptionString(it) }
            }
        }
    }
}

class PollOptionDate : PollOption, DatabaseEntity {
    val dateStartTimestamp: Long
    val dateEndTimestamp: Long?
    override val pollID: tPollID
    override val id: tOptionID

    constructor(dateStartTimestamp: Long, dateEndTimestamp: Long?, pollID: tPollID, id: tOptionID) {
        this.dateStartTimestamp = dateStartTimestamp
        this.dateEndTimestamp = dateEndTimestamp
        this.pollID = pollID
        this.id = id
    }

    private constructor(optionRow: ResultRow) {
        id = optionRow[PollOptionDate.id]
        pollID = optionRow[PollOptionDate.pollID]
        dateStartTimestamp = optionRow[PollOptionDate.dateStartTimestamp]
        dateEndTimestamp = optionRow[PollOptionDate.dateEndTimestamp]
    }

    override fun save() {
        TODO("Not yet implemented")
    }

    companion object : Table("poll_option_date_time") {
        val id = integer("id").autoIncrement()
        val dateStartTimestamp = long("dateStartTimestamp")
        val dateEndTimestamp = long("dateEndTimestamp").nullable()
        val pollID = varchar("pollId", UUIDLength)

        override val primaryKey = PrimaryKey(id)

        fun fromPollIDAndID(pollID: tPollID, optionID: tOptionID): PollOptionDate? {
            return transaction {
                val result =
                    PollOptionDate.select { (PollOptionDate.pollID eq pollID) and (PollOptionDate.id eq optionID) }
                        .firstOrNull()
                return@transaction result?.let { PollOptionDate(it) }
            }
        }
    }
}

class PollOptionDateTime : PollOption, DatabaseEntity {
    val dateTimeStartTimestamp: Long
    val dateTImeEndTimestamp: Long?
    override val pollID: tPollID
    override val id: tOptionID

    constructor(dateTimeStartTimestamp: Long, dateTImeEndTimestamp: Long?, pollID: tPollID, id: tOptionID) {
        this.dateTimeStartTimestamp = dateTimeStartTimestamp
        this.dateTImeEndTimestamp = dateTImeEndTimestamp
        this.pollID = pollID
        this.id = id
    }

    private constructor(optionRow: ResultRow) {
        id = optionRow[PollOptionDateTime.id]
        pollID = optionRow[PollOptionDateTime.pollID]
        dateTimeStartTimestamp = optionRow[PollOptionDateTime.dateTimeStartTimestamp]
        dateTImeEndTimestamp = optionRow[PollOptionDateTime.dateTimeEndTimestamp]
    }

    override fun save() {
        TODO("Not yet implemented")
    }

    companion object : Table("poll_option_date_time") {
        val id = integer("id").autoIncrement()
        val dateTimeStartTimestamp = long("dateTimeStartTimestamp")
        val dateTimeEndTimestamp = long("dateTimeEndTimestamp").nullable()
        val pollID = varchar("pollId", UUIDLength)

        override val primaryKey = PrimaryKey(id)

        fun fromPollIDAndID(pollID: tPollID, optionID: tOptionID): PollOptionDateTime? {
            return transaction {
                val result =
                    PollOptionDateTime.select { (PollOptionDateTime.pollID eq pollID) and (PollOptionDateTime.id eq optionID) }
                        .firstOrNull()
                return@transaction result?.let { PollOptionDateTime(it) }
            }
        }
    }
}