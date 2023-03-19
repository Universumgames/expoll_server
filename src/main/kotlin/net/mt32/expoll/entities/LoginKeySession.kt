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


@Deprecated("Use OTP and session instead")
class LoginKeySession : DatabaseEntity {
    val loginKey: String
    val expirationTimestamp: UnixTimestamp
    var userAgent: String?
    val userID: tUserID

    constructor(loginKey: String, userID: tUserID, expirationTimestamp: UnixTimestamp, userAgent: String? = null) {
        this.loginKey = loginKey
        this.expirationTimestamp = expirationTimestamp
        this.userAgent = userAgent
        this.userID = userID
    }

    constructor(sessionRow: ResultRow) {
        this.loginKey = sessionRow[LoginKeySession.loginKey]
        this.expirationTimestamp = sessionRow[LoginKeySession.expirationTimestamp].toUnixTimestampFromDB()
        this.userAgent = sessionRow[LoginKeySession.userAgent]
        this.userID = sessionRow[LoginKeySession.userID]
    }


    companion object : Table("session") {
        val loginKey = varchar("loginKey", UUIDLength)
        val expirationTimestamp = long("expirationTimestamp")
        val userAgent = varchar("userAgent", 512).nullable()
        val userID = varchar("userId", UUIDLength)

        override val primaryKey = PrimaryKey(loginKey)

        fun fromLoginKey(loginKey: String): LoginKeySession? {
            return transaction {
                val loginKeySessionRow =
                    LoginKeySession.select { LoginKeySession.loginKey eq loginKey }.firstOrNull()
                        ?: return@transaction null
                return@transaction LoginKeySession(loginKeySessionRow)
            }
        }

        fun forUser(userID: tUserID): List<LoginKeySession> {
            return transaction {
                val loginKeySessionRow =
                    LoginKeySession.select { LoginKeySession.userID eq userID }
                return@transaction loginKeySessionRow.map { LoginKeySession(it) }
            }
        }

        /**
         * creates a new session for a user,
         * does not save it to the database
         * @param forUserID the user the session should be created for
         * @return new Session object with an predefined expiration
         */
        fun createSession(forUserID: tUserID): LoginKeySession {
            var loginKey = ""
            transaction {
                do {
                    loginKey = UUID.randomUUID().toString()
                } while (fromLoginKey(loginKey) != null)
            }
            return LoginKeySession(loginKey, forUserID, UnixTimestamp.now().addDays(120))
        }

        fun fromShortKey(shortKey: String, userID: tUserID): LoginKeySession? {
            return transaction {
                val loginKeySessionRow =
                    LoginKeySession.select { (LoginKeySession.loginKey like ("$shortKey%")) and (LoginKeySession.userID eq userID) }
                        .firstOrNull()
                return@transaction loginKeySessionRow?.let { LoginKeySession(it) }
            }
        }
    }

    override fun save(): Boolean {
        transaction {
            LoginKeySession.upsert(LoginKeySession.loginKey) {
                it[loginKey] = this@LoginKeySession.loginKey
                it[expirationTimestamp] = this@LoginKeySession.expirationTimestamp.toDB()
                it[userAgent] = this@LoginKeySession.userAgent
                it[userID] = this@LoginKeySession.userID
            }
        }
        return true
    }

    override fun delete(): Boolean {
        transaction {
            LoginKeySession.deleteWhere {
                LoginKeySession.loginKey eq this@LoginKeySession.loginKey
            }
        }
        return true
    }

    fun asSafeSession(currentLoginKey: String? = null): SafeSession {
        return SafeSession(
            expirationTimestamp.toClient(),
            userAgent,
            loginKey.substring(0, 4),
            loginKey.equals(currentLoginKey, ignoreCase = true)
        )
    }
}

