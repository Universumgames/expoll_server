package net.mt32.expoll.entities

import net.mt32.expoll.commons.helper.UnixTimestamp
import net.mt32.expoll.commons.helper.toUnixTimestampFromDB
import net.mt32.expoll.commons.serializable.responses.ComplexOption
import net.mt32.expoll.commons.tOptionID
import net.mt32.expoll.commons.tPollID
import net.mt32.expoll.database.DatabaseEntity
import net.mt32.expoll.database.IDatabaseEntity
import net.mt32.expoll.database.UUIDLength
import net.mt32.expoll.helper.upsertCustom
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

interface PollOption : IDatabaseEntity {
    val pollID: tPollID
    val id: tOptionID

    fun toComplexOption(): ComplexOption

    fun toNotificationString(useUTC: Boolean = false): String
}

class PollOptionString : PollOption, DatabaseEntity {
    val value: String
    override val pollID: tPollID
    override var id: tOptionID

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
        id = transaction {
            return@transaction PollOptionString.upsertCustom(PollOptionString.id) {
                it[id] = this@PollOptionString.id
                it[pollID] = this@PollOptionString.pollID
                it[value] = this@PollOptionString.value
            }.resultedValues?.firstOrNull()?.get(PollOptionString.id)
        } ?: id
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

    override fun toNotificationString(useUTC: Boolean): String {
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
                    PollOptionString.selectAll()
                        .where { (PollOptionString.pollID eq pollID) and (PollOptionString.id eq optionID) }
                        .firstOrNull()
                return@transaction result?.let { PollOptionString(it) }
            }
        }

        fun fromPollID(pollID: tPollID): List<PollOptionString> {
            return transaction {
                val result =
                    PollOptionString.selectAll().where { PollOptionString.pollID eq pollID }
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

interface TimedPollOption : PollOption {
    val ianaTimezone: String
}

class PollOptionDate : TimedPollOption, DatabaseEntity {
    val dateStartTimestamp: UnixTimestamp
    val dateEndTimestamp: UnixTimestamp?
    override val ianaTimezone: String
    override val pollID: tPollID
    override var id: tOptionID

    constructor(
        dateStartTimestamp: UnixTimestamp,
        dateEndTimestamp: UnixTimestamp?,
        timeZone: String,
        pollID: tPollID,
        id: tOptionID
    ) {
        this.dateStartTimestamp = dateStartTimestamp
        this.dateEndTimestamp = dateEndTimestamp
        this.ianaTimezone = timeZone
        this.pollID = pollID
        this.id = id
    }

    private constructor(optionRow: ResultRow) {
        id = optionRow[PollOptionDate.id]
        pollID = optionRow[PollOptionDate.pollID]
        dateStartTimestamp = optionRow[PollOptionDate.dateStartTimestamp].toUnixTimestampFromDB()
        dateEndTimestamp = optionRow[PollOptionDate.dateEndTimestamp]?.toUnixTimestampFromDB()
        ianaTimezone = optionRow[PollOptionDate.timeZone]
    }

    override fun save(): Boolean {
        id = transaction {
            return@transaction PollOptionDate.upsertCustom(PollOptionDate.id) {
                it[id] = this@PollOptionDate.id
                it[pollID] = this@PollOptionDate.pollID
                it[dateStartTimestamp] = this@PollOptionDate.dateStartTimestamp.toDB()
                it[dateEndTimestamp] = this@PollOptionDate.dateEndTimestamp?.toDB()
                it[timeZone] = this@PollOptionDate.ianaTimezone
            }.resultedValues?.firstOrNull()?.get(PollOptionDate.id)
        } ?: id
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
            id,
            dateStart = dateStartTimestamp.toClient(),
            dateEnd = dateEndTimestamp?.toClient(),
            timezone = ianaTimezone,
        )
    }

    override fun toNotificationString(useUTC: Boolean): String {
        return dateStartTimestamp.toDateString(if (useUTC) null else ianaTimezone)
    }

    companion object : Table("poll_option_date") {
        val id = integer("id").autoIncrement()
        val pollID = varchar("pollId", UUIDLength)

        val dateStartTimestamp = long("dateStartTimestamp")
        val dateEndTimestamp = long("dateEndTimestamp").nullable()
        val timeZone = varchar("timeZone", 50)

        override val primaryKey = PrimaryKey(id)

        fun fromPollIDAndID(pollID: tPollID, optionID: tOptionID): PollOptionDate? {
            return transaction {
                val result =
                    selectAll().where { (PollOptionDate.pollID eq pollID) and (PollOptionDate.id eq optionID) }
                        .firstOrNull()
                return@transaction result?.let { PollOptionDate(it) }
            }
        }

        fun fromPollID(pollID: tPollID): List<PollOptionDate> {
            return transaction {
                val result =
                    selectAll().where { PollOptionDate.pollID eq pollID }
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

class PollOptionDateTime : TimedPollOption, DatabaseEntity {
    val dateTimeStartTimestamp: UnixTimestamp
    val dateTimeEndTimestamp: UnixTimestamp?
    override val ianaTimezone: String
    override val pollID: tPollID
    override var id: tOptionID

    constructor(
        dateTimeStartTimestamp: UnixTimestamp,
        dateTImeEndTimestamp: UnixTimestamp?,
        timeZone: String,
        pollID: tPollID,
        id: tOptionID
    ) {
        this.dateTimeStartTimestamp = dateTimeStartTimestamp
        this.dateTimeEndTimestamp = dateTImeEndTimestamp
        this.ianaTimezone = timeZone
        this.pollID = pollID
        this.id = id
    }

    private constructor(optionRow: ResultRow) {
        id = optionRow[PollOptionDateTime.id]
        pollID = optionRow[PollOptionDateTime.pollID]
        dateTimeStartTimestamp = optionRow[PollOptionDateTime.dateTimeStartTimestamp].toUnixTimestampFromDB()
        dateTimeEndTimestamp = optionRow[PollOptionDateTime.dateTimeEndTimestamp]?.toUnixTimestampFromDB()
        ianaTimezone = optionRow[PollOptionDateTime.timeZone]
    }

    override fun save(): Boolean {
        id = transaction {
            return@transaction upsertCustom(PollOptionDateTime.id) {
                it[id] = this@PollOptionDateTime.id
                it[pollID] = this@PollOptionDateTime.pollID
                it[PollOptionDateTime.dateTimeStartTimestamp] = this@PollOptionDateTime.dateTimeStartTimestamp.toDB()
                it[PollOptionDateTime.dateTimeEndTimestamp] = this@PollOptionDateTime.dateTimeEndTimestamp?.toDB()
                it[timeZone] = this@PollOptionDateTime.ianaTimezone
            }.resultedValues?.firstOrNull()?.get(PollOptionDateTime.id)
        } ?: id
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
            id,
            dateTimeStart = dateTimeStartTimestamp.toClient(),
            dateTimeEnd = dateTimeEndTimestamp?.toClient(),
            timezone = ianaTimezone
        )
    }

    override fun toNotificationString(useUTC: Boolean): String {
        return dateTimeStartTimestamp.toDateTimeString(if(useUTC) null else ianaTimezone)
    }

    companion object : Table("poll_option_date_time") {
        val id = integer("id").autoIncrement()
        val pollID = varchar("pollId", UUIDLength)

        val dateTimeStartTimestamp = long("dateTimeStartTimestamp")
        val dateTimeEndTimestamp = long("dateTimeEndTimestamp").nullable()
        val timeZone = varchar("timeZone", 50)

        override val primaryKey = PrimaryKey(id)

        fun fromPollIDAndID(pollID: tPollID, optionID: tOptionID): PollOptionDateTime? {
            return transaction {
                val result =
                    selectAll().where { (PollOptionDateTime.pollID eq pollID) and (PollOptionDateTime.id eq optionID) }
                        .firstOrNull()
                return@transaction result?.let { PollOptionDateTime(it) }
            }
        }

        fun fromPollID(pollID: tPollID): List<PollOptionDateTime> {
            return transaction {
                val result =
                    selectAll().where { PollOptionDateTime.pollID eq pollID }
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