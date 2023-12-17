package net.mt32.expoll.entities

import net.mt32.expoll.database.DatabaseEntity
import net.mt32.expoll.database.IDatabaseEntity
import net.mt32.expoll.database.UUIDLength
import net.mt32.expoll.helper.UnixTimestamp
import net.mt32.expoll.helper.toUnixTimestampFromDB
import net.mt32.expoll.helper.upsertCustom
import net.mt32.expoll.serializable.responses.ComplexOption
import net.mt32.expoll.tOptionID
import net.mt32.expoll.tPollID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

interface PollOption : IDatabaseEntity {
    val pollID: tPollID
    val id: tOptionID

    fun toComplexOption(): ComplexOption

    override fun toString(): String
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
            PollOptionString.upsertCustom(PollOptionString.id) {
                it[id] = this@PollOptionString.id
                it[pollID] = this@PollOptionString.pollID
                it[value] = this@PollOptionString.value
            }
        }
        return true
    }

    override fun delete(): Boolean {
        transaction {
            Vote.deleteWhere {
                (Vote.pollID eq this@PollOptionString.pollID) and
                        (Vote.optionID eq this@PollOptionString.id)
            }
            deleteWhere {
                id eq this@PollOptionString.id
            }
        }
        return true
    }

    override fun toComplexOption(): ComplexOption {
        return ComplexOption(
            id, value = value
        )
    }

    override fun toString(): String {
        return value
    }

    companion object : Table("poll_option_string") {
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

        fun newID(pollID: tPollID): Int {
            return transaction {
                var id = 0
                while (fromPollIDAndID(pollID, id) != null) {
                    id++
                }
                return@transaction id
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
        dateStartTimestamp = optionRow[PollOptionDate.dateStartTimestamp].toUnixTimestampFromDB()
        dateEndTimestamp = optionRow[PollOptionDate.dateEndTimestamp]?.toUnixTimestampFromDB()
    }

    override fun save(): Boolean {
        transaction {
            PollOptionDate.upsertCustom(PollOptionDate.id) {
                it[id] = this@PollOptionDate.id
                it[pollID] = this@PollOptionDate.pollID
                it[dateStartTimestamp] = this@PollOptionDate.dateStartTimestamp.toDB()
                it[dateEndTimestamp] = this@PollOptionDate.dateEndTimestamp?.toDB()
            }
        }
        return true
    }

    override fun delete(): Boolean {
        transaction {
            Vote.deleteWhere {
                (Vote.pollID eq this@PollOptionDate.pollID) and
                        (Vote.optionID eq this@PollOptionDate.id)
            }
            deleteWhere {
                id eq this@PollOptionDate.id
            }
        }
        return true
    }

    override fun toComplexOption(): ComplexOption {
        return ComplexOption(
            id, dateStart = dateStartTimestamp.toClient(), dateEnd = dateEndTimestamp?.toClient()
        )
    }

    override fun toString(): String {
        return dateStartTimestamp.toString()
    }

    companion object : Table("poll_option_date") {
        val id = integer("id").autoIncrement()
        val dateStartTimestamp = long("dateStartTimestamp")
        val dateEndTimestamp = long("dateEndTimestamp").nullable()
        val pollID = varchar("pollId", UUIDLength)

        override val primaryKey = PrimaryKey(id)

        fun fromPollIDAndID(pollID: tPollID, optionID: tOptionID): PollOptionDate? {
            return transaction {
                val result =
                    select { (PollOptionDate.pollID eq pollID) and (PollOptionDate.id eq optionID) }
                        .firstOrNull()
                return@transaction result?.let { PollOptionDate(it) }
            }
        }

        fun fromPollID(pollID: tPollID): List<PollOptionDate> {
            return transaction {
                val result =
                    select { PollOptionDate.pollID eq pollID }
                return@transaction result.map { PollOptionDate(it) }
            }
        }

        fun newID(pollID: tPollID): Int {
            return transaction {
                var id = 0
                while (fromPollIDAndID(pollID, id) != null) {
                    id++
                }
                return@transaction id
            }
        }
    }
}

class PollOptionDateTime : PollOption, DatabaseEntity {
    val dateTimeStartTimestamp: UnixTimestamp
    val dateTimeEndTimestamp: UnixTimestamp?
    override val pollID: tPollID
    override val id: tOptionID

    constructor(
        dateTimeStartTimestamp: UnixTimestamp,
        dateTImeEndTimestamp: UnixTimestamp?,
        pollID: tPollID,
        id: tOptionID
    ) {
        this.dateTimeStartTimestamp = dateTimeStartTimestamp
        this.dateTimeEndTimestamp = dateTImeEndTimestamp
        this.pollID = pollID
        this.id = id
    }

    private constructor(optionRow: ResultRow) {
        id = optionRow[PollOptionDateTime.id]
        pollID = optionRow[PollOptionDateTime.pollID]
        dateTimeStartTimestamp = optionRow[PollOptionDateTime.dateTimeStartTimestamp].toUnixTimestampFromDB()
        dateTimeEndTimestamp = optionRow[PollOptionDateTime.dateTimeEndTimestamp]?.toUnixTimestampFromDB()
    }

    override fun save(): Boolean {
        transaction {
            upsertCustom(PollOptionDateTime.id) {
                it[id] = this@PollOptionDateTime.id
                it[pollID] = this@PollOptionDateTime.pollID
                it[PollOptionDateTime.dateTimeStartTimestamp] = this@PollOptionDateTime.dateTimeStartTimestamp.toDB()
                it[PollOptionDateTime.dateTimeEndTimestamp] = this@PollOptionDateTime.dateTimeEndTimestamp?.toDB()
            }
        }
        return true
    }

    override fun delete(): Boolean {
        transaction {
            Vote.deleteWhere {
                (Vote.pollID eq this@PollOptionDateTime.pollID) and
                        (Vote.optionID eq this@PollOptionDateTime.id)
            }
            deleteWhere {
                id eq this@PollOptionDateTime.id
            }
        }
        return true
    }

    override fun toComplexOption(): ComplexOption {
        return ComplexOption(
            id, dateTimeStart = dateTimeStartTimestamp.toClient(), dateTimeEnd = dateTimeEndTimestamp?.toClient()
        )
    }

    override fun toString(): String {
        return dateTimeStartTimestamp.toString()
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
                    select { (PollOptionDateTime.pollID eq pollID) and (PollOptionDateTime.id eq optionID) }
                        .firstOrNull()
                return@transaction result?.let { PollOptionDateTime(it) }
            }
        }

        fun fromPollID(pollID: tPollID): List<PollOptionDateTime> {
            return transaction {
                val result =
                    select { PollOptionDateTime.pollID eq pollID }
                return@transaction result.map { PollOptionDateTime(it) }
            }
        }

        fun newID(pollID: tPollID): Int {
            return transaction {
                var id = 0
                while (fromPollIDAndID(pollID, id) != null) {
                    id++
                }
                return@transaction id
            }
        }
    }
}