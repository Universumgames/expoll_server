package net.mt32.expoll.entities

import net.mt32.expoll.database.DatabaseEntity
import net.mt32.expoll.database.IDatabaseEntity
import net.mt32.expoll.database.UUIDLength
import net.mt32.expoll.helper.UnixTimestamp
import net.mt32.expoll.helper.toUnixTimestamp
import net.mt32.expoll.helper.upsert
import net.mt32.expoll.tOptionID
import net.mt32.expoll.tPollID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

interface PollOption : IDatabaseEntity {
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

    override fun save(): Boolean {
        transaction {
            PollOptionString.upsert(PollOptionString.id) {
                it[id] = this@PollOptionString.id
                it[pollID] = this@PollOptionString.pollID
                it[value] = this@PollOptionString.value
            }
        }
        return true
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

        fun fromPollID(pollID: tPollID): List<PollOptionString> {
            return transaction {
                val result =
                    PollOptionString.select { PollOptionString.pollID eq pollID }
                return@transaction result.map { PollOptionString(it) }
            }
        }
    }
}

class PollOptionDate : PollOption, DatabaseEntity {
    val dateStartTimestamp: UnixTimestamp
    val dateEndTimestamp: UnixTimestamp?
    override val pollID: tPollID
    override val id: tOptionID

    constructor(dateStartTimestamp: UnixTimestamp, dateEndTimestamp: UnixTimestamp?, pollID: tPollID, id: tOptionID) {
        this.dateStartTimestamp = dateStartTimestamp
        this.dateEndTimestamp = dateEndTimestamp
        this.pollID = pollID
        this.id = id
    }

    private constructor(optionRow: ResultRow) {
        id = optionRow[PollOptionDate.id]
        pollID = optionRow[PollOptionDate.pollID]
        dateStartTimestamp = optionRow[PollOptionDate.dateStartTimestamp].toUnixTimestamp()
        dateEndTimestamp = optionRow[PollOptionDate.dateEndTimestamp]?.toUnixTimestamp()
    }

    override fun save(): Boolean {
        transaction {
            PollOptionDate.upsert(PollOptionDate.id) {
                it[id] = this@PollOptionDate.id
                it[pollID] = this@PollOptionDate.pollID
                it[dateStartTimestamp] = this@PollOptionDate.dateStartTimestamp.toLong()
                it[dateEndTimestamp] = this@PollOptionDate.dateEndTimestamp?.toLong()
            }
        }
        return true
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

        fun fromPollID(pollID: tPollID): List<PollOptionDate> {
            return transaction {
                val result =
                    PollOptionDate.select { PollOptionDate.pollID eq pollID }
                return@transaction result.map { PollOptionDate(it) }
            }
        }
    }
}

class PollOptionDateTime : PollOption, DatabaseEntity {
    val dateTimeStartTimestamp: UnixTimestamp
    val dateTimeEndTimestamp: UnixTimestamp?
    override val pollID: tPollID
    override val id: tOptionID

    constructor(dateTimeStartTimestamp: UnixTimestamp, dateTImeEndTimestamp: UnixTimestamp?, pollID: tPollID, id: tOptionID) {
        this.dateTimeStartTimestamp = dateTimeStartTimestamp
        this.dateTimeEndTimestamp = dateTImeEndTimestamp
        this.pollID = pollID
        this.id = id
    }

    private constructor(optionRow: ResultRow) {
        id = optionRow[PollOptionDateTime.id]
        pollID = optionRow[PollOptionDateTime.pollID]
        dateTimeStartTimestamp = optionRow[PollOptionDateTime.dateTimeStartTimestamp].toUnixTimestamp()
        dateTimeEndTimestamp = optionRow[PollOptionDateTime.dateTimeEndTimestamp]?.toUnixTimestamp()
    }

    override fun save(): Boolean {
        transaction {
            PollOptionDateTime.upsert(PollOptionDateTime.id) {
                it[id] = this@PollOptionDateTime.id
                it[pollID] = this@PollOptionDateTime.pollID
                it[dateTimeStartTimestamp] = this@PollOptionDateTime.dateTimeStartTimestamp.toLong()
                it[dateTimeEndTimestamp] = this@PollOptionDateTime.dateTimeEndTimestamp?.toLong()
            }
        }
        return true
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

        fun fromPollID(pollID: tPollID): List<PollOptionDateTime> {
            return transaction {
                val result =
                    PollOptionDateTime.select { PollOptionDateTime.pollID eq pollID }
                return@transaction result.map { PollOptionDateTime(it) }
            }
        }
    }
}