package net.mt32.expoll.entities

import net.mt32.expoll.database.DatabaseEntity
import net.mt32.expoll.database.UUIDLength
import net.mt32.expoll.commons.helper.UnixTimestamp
import net.mt32.expoll.commons.helper.toUnixTimestampFromDB
import net.mt32.expoll.helper.upsertCustom
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction


class AppAttest : DatabaseEntity {
    val uuid: String
    val challenge: String
    val createdAtTimestamp: UnixTimestamp

    constructor(uuid: String, challenge: String, createdAtTimestamp: UnixTimestamp) : super() {
        this.uuid = uuid
        this.challenge = challenge
        this.createdAtTimestamp = createdAtTimestamp
    }

    private constructor(attestRow: ResultRow) {
        this.uuid = attestRow[AppAttest.uuid]
        this.challenge = attestRow[AppAttest.challenge]
        this.createdAtTimestamp = attestRow[AppAttest.createdAtTimestamp].toUnixTimestampFromDB()
    }

    override fun save(): Boolean {
        transaction {
            AppAttest.upsertCustom(AppAttest.uuid) {
                it[uuid] = this@AppAttest.uuid
                it[challenge] = this@AppAttest.challenge
                it[createdAtTimestamp] = this@AppAttest.createdAtTimestamp.toDB()
            }
        }
        return true
    }

    override fun delete(): Boolean {
        transaction {
            AppAttest.deleteWhere {
                AppAttest.uuid eq this@AppAttest.uuid
            }
        }
        return true
    }

    companion object : Table("appleAppAttests") {
        val uuid = varchar("uuid", UUIDLength)
        val challenge = varchar("challenge", 255)
        val createdAtTimestamp = long("createdAtTimestamp")

        override val primaryKey = PrimaryKey(uuid)

        fun fromUUID(uuid: String): AppAttest? {
            return transaction {
                val attestRow = AppAttest.selectAll().where { AppAttest.uuid eq uuid }.firstOrNull()
                return@transaction attestRow?.let { AppAttest(it) }
            }
        }
    }
}