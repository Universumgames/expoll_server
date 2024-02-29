package net.mt32.expoll.entities

import net.mt32.expoll.config
import net.mt32.expoll.database.DatabaseEntity
import net.mt32.expoll.database.UUIDLength
import net.mt32.expoll.helper.UnixTimestamp
import net.mt32.expoll.helper.toBase64
import net.mt32.expoll.helper.toUnixTimestampFromDB
import net.mt32.expoll.helper.upsertCustom
import net.mt32.expoll.notification.ExpollNotificationHandler
import net.mt32.expoll.serializable.request.Platform
import net.mt32.expoll.tUserID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.ThreadLocalRandom

class OTP : DatabaseEntity {
    val otp: String
    val userID: tUserID
    val expirationTimestamp: UnixTimestamp
    val forApp: Boolean

    val valid: Boolean
        get() = expirationTimestamp > UnixTimestamp.now()

    constructor(otp: String, userID: tUserID, forApp: Boolean, expirationTimestamp: UnixTimestamp = UnixTimestamp.now().addHours(1)) {
        this.otp = otp
        this.userID = userID
        this.forApp = forApp
        this.expirationTimestamp = expirationTimestamp
    }

    private constructor(resultRow: ResultRow) {
        this.otp = resultRow[OTP.otp]
        this.userID = resultRow[OTP.userID]
        this.forApp = resultRow[OTP.forApp] ?: false
        this.expirationTimestamp = resultRow[OTP.expirationTimestamp].toUnixTimestampFromDB()
    }

    companion object : Table("otp") {
        val otp = varchar("otp", 64)
        val userID = varchar("userid", UUIDLength)
        val forApp = bool("forApp").default(false)
        val expirationTimestamp = long("expirationTimestamp")

        fun fromOTP(otp: String): OTP? {
            if (otp == config.testUser.otp) {
                val testuser = User.byUsername(config.testUser.username) ?: return null
                return OTP(otp, testuser.id, true)
            }
            return transaction {
                val otp = OTP.selectAll().where { OTP.otp eq otp }.firstOrNull()?.let { OTP(it) }
                if (otp != null && !otp.valid) {
                    otp.delete()
                    return@transaction null
                }
                return@transaction otp
            }
        }

        private fun randomOTP(): String {
            var otp: String
            do {
                val bytes = ByteArray(config.otpBaseLength)
                ThreadLocalRandom.current().nextBytes(bytes)
                otp = bytes.toBase64()
            } while (fromOTP(otp) != null)
            return otp
        }

        /**
         * Creates and saves a new OTP for a user
         */
        fun create(userID: tUserID, forApp: Boolean): OTP {
            val otp = OTP(randomOTP(), userID, forApp)
            otp.save()
            return otp
        }

        fun fromUser(userID: tUserID): List<OTP> {
            return transaction {
                val result = OTP.selectAll().where { OTP.userID eq userID }
                return@transaction result.map { OTP(it) }
            }
        }

        fun all(): List<OTP> {
            return transaction {
                return@transaction OTP.selectAll().map { OTP(it) }
            }
        }
    }

    override fun save(): Boolean {
        transaction {
            OTP.upsertCustom(OTP.otp) {
                it[otp] = this@OTP.otp
                it[userID] = this@OTP.userID
                it[forApp] = this@OTP.forApp
                it[expirationTimestamp] = this@OTP.expirationTimestamp.toDB()
            }
        }
        return true
    }

    override fun delete(): Boolean {
        transaction {
            OTP.deleteWhere { otp eq this@OTP.otp }
        }
        return true
    }

    fun createSessionAndDeleteSelf(userAgent: String, clientVersion: String?, platform: Platform): Session {
        val session = Session(
            userID,
            userAgent
        )
        session.clientVersion = clientVersion
        session.platform = platform
        session.save()
        delete()
        ExpollNotificationHandler.sendNewLogin(session.user!!)
        return session
    }
}