package net.mt32.expoll.entities

import net.mt32.expoll.UUIDLength
import net.mt32.expoll.helper.timestampFromString
import net.mt32.expoll.tUserID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction


class Session {
    val loginkey: String
    val expirationTimestamp: Long
    var userAgent: String?
    val userID: tUserID

    constructor(loginkey: String, expirationTimestamp: Long, userAgent: String?, userID: tUserID) {
        this.loginkey = loginkey
        this.expirationTimestamp = expirationTimestamp
        this.userAgent = userAgent
        this.userID = userID
    }

    constructor(sessionRow: ResultRow) {
        this.loginkey = sessionRow[Sessions.loginKey]
        this.expirationTimestamp = timestampFromString(sessionRow[Sessions.expirationTimestamp])
        this.userAgent = sessionRow[Sessions.userAgent]
        this.userID = sessionRow[Sessions.userID]
    }


    companion object {
        fun fromLoginKey(loginKey: String): Session? {
            return transaction {
                val sessionRow =
                    Sessions.select { Sessions.loginKey eq loginKey }.firstOrNull() ?: return@transaction null
                return@transaction Session(sessionRow)
            }
        }
    }
}

object Sessions : Table("session") {
    val loginKey = varchar("loginKey", UUIDLength)
    val expirationTimestamp = varchar("expiration", 255)
    val userAgent = varchar("userAgent", 512).nullable()
    val userID = varchar("userId", UUIDLength)

    override val primaryKey = PrimaryKey(loginKey)
}