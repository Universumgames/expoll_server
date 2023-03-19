package net.mt32.expoll.entities

import net.mt32.expoll.database.DatabaseEntity
import net.mt32.expoll.database.UUIDLength
import net.mt32.expoll.helper.UnixTimestamp
import net.mt32.expoll.helper.toUnixTimestampFromDB
import net.mt32.expoll.helper.upsert
import net.mt32.expoll.serializable.responses.SafeSession
import net.mt32.expoll.tUserID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*


class Session : DatabaseEntity {
    val loginkey: String
    val expirationTimestamp: UnixTimestamp
    var userAgent: String?
    val userID: tUserID

    constructor(loginkey: String, userID: tUserID, expirationTimestamp: UnixTimestamp, userAgent: String? = null) {
        this.loginkey = loginkey
        this.expirationTimestamp = expirationTimestamp
        this.userAgent = userAgent
        this.userID = userID
    }

    constructor(sessionRow: ResultRow) {
        this.loginkey = sessionRow[Session.loginKey]
        this.expirationTimestamp = sessionRow[Session.expirationTimestamp].toUnixTimestampFromDB()
        this.userAgent = sessionRow[Session.userAgent]
        this.userID = sessionRow[Session.userID]
    }


    companion object : Table("session") {
        val loginKey = varchar("loginKey", UUIDLength)
        val expirationTimestamp = long("expirationTimestamp")
        val userAgent = varchar("userAgent", 512).nullable()
        val userID = varchar("userId", UUIDLength)

        override val primaryKey = PrimaryKey(loginKey)

        fun fromLoginKey(loginKey: String): Session? {
            return transaction {
                val sessionRow =
                    Session.select { Session.loginKey eq loginKey }.firstOrNull() ?: return@transaction null
                return@transaction Session(sessionRow)
            }
        }

        fun forUser(userID: tUserID): List<Session> {
            return transaction {
                val sessionRow =
                    Session.select { Session.userID eq userID }
                return@transaction sessionRow.map { Session(it) }
            }
        }

        /**
         * creates a new session for a user,
         * does not save it to the database
         * @param forUserID the user the session should be created for
         * @return new Session object with an predefined expiration
         */
        fun createSession(forUserID: tUserID): Session {
            var loginKey = ""
            transaction {
                do {
                    loginKey = UUID.randomUUID().toString()
                } while (fromLoginKey(loginKey) != null)
            }
            return Session(loginKey, forUserID, UnixTimestamp.now().addDays(120))
        }

        fun fromShortKey(shortKey: String, userID: tUserID): Session? {
            return transaction {
                val sessionRow =
                    Session.select { (Session.loginKey like ("$shortKey%")) and (Session.userID eq userID) }
                        .firstOrNull()
                return@transaction sessionRow?.let { Session(it) }
            }
        }
    }

    override fun save(): Boolean {
        transaction {
            Session.upsert(Session.loginKey) {
                it[loginKey] = this@Session.loginkey
                it[expirationTimestamp] = this@Session.expirationTimestamp.toDB()
                it[userAgent] = this@Session.userAgent
                it[userID] = this@Session.userID
            }
        }
        return true
    }

    override fun delete(): Boolean {
        transaction {
            Session.deleteWhere {
                Session.loginKey eq this@Session.loginkey
            }
        }
        return true
    }

    fun asSafeSession(currentLoginKey: String? = null): SafeSession {
        return SafeSession(
            expirationTimestamp.toClient(),
            userAgent,
            loginkey.substring(0, 4),
            loginkey.equals(currentLoginKey, ignoreCase = true)
        )
    }
}

class OTP: DatabaseEntity{
    val otp: String
    val userID: tUserID
    val expirationTimestamp: UnixTimestamp

    constructor(otp: String, userID: tUserID, expirationTimestamp: UnixTimestamp = UnixTimestamp.now().addHours(1)) {
        this.otp = otp
        this.userID = userID
        this.expirationTimestamp = expirationTimestamp
    }

    private constructor(resultRow: ResultRow){
        this.otp = resultRow[OTP.otp]
        this.userID = resultRow[OTP.userID]
        this.expirationTimestamp = resultRow[OTP.expirationTimestamp].toUnixTimestampFromDB()
    }

    companion object: Table("otp"){
        val otp = varchar("otp", 16)
        val userID = varchar("userid", UUIDLength)
        val expirationTimestamp = long("expirationTimestamp")
    }

    override fun save(): Boolean {
        transaction {
            OTP.upsert(OTP.otp) {
                it[OTP.otp] = this@OTP.otp
                it[OTP.userID] = this@OTP.userID
                it[OTP.expirationTimestamp] = this@OTP.expirationTimestamp.toDB()
            }
        }
        return true
    }

    override fun delete(): Boolean {
        transaction {
            OTP.deleteWhere { OTP.otp eq this@OTP.otp }
        }
        return true
    }
}