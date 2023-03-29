package net.mt32.expoll.entities

import net.mt32.expoll.database.DatabaseEntity
import net.mt32.expoll.database.UUIDLength
import net.mt32.expoll.helper.UnixTimestamp
import net.mt32.expoll.helper.toUnixTimestampFromDB
import net.mt32.expoll.helper.upsert
import net.mt32.expoll.tUserID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction


class APNDevice : DatabaseEntity {
    val deviceID: String
    var userID: tUserID
    val creationTimestamp: UnixTimestamp
    val session: Session?
        get() = Session.fromNonce(sessionNonce)

    var sessionNonce: Long

    constructor(
        deviceID: String,
        userID: tUserID,
        creationTimestamp: UnixTimestamp = UnixTimestamp.now(),
        sessionNonce: Long
    ) {
        this.deviceID = deviceID
        this.userID = userID
        this.creationTimestamp = creationTimestamp
        this.sessionNonce = sessionNonce
    }

    private constructor(apnDeviceRow: ResultRow) {
        this.deviceID = apnDeviceRow[APNDevice.deviceID]
        this.userID = apnDeviceRow[APNDevice.userID]
        this.creationTimestamp = apnDeviceRow[APNDevice.creationTimestamp].toUnixTimestampFromDB()
        this.sessionNonce = apnDeviceRow[APNDevice.sessionNonce]
    }

    override fun save(): Boolean {
        transaction {
            APNDevice.upsert(APNDevice.deviceID) {
                it[deviceID] = this@APNDevice.deviceID
                it[userID] = this@APNDevice.userID
                it[creationTimestamp] = this@APNDevice.creationTimestamp.toDB()
                it[sessionNonce] = this@APNDevice.sessionNonce
            }
        }
        return true
    }

    override fun delete(): Boolean {
        transaction {
            APNDevice.deleteWhere {
                APNDevice.deviceID eq this@APNDevice.deviceID
            }
        }
        return true
    }

    companion object : Table("apn_devices") {
        val deviceID = varchar("deviceId", 255)
        val userID = varchar("userId", UUIDLength)
        val creationTimestamp = long("creationTimestamp")
        val sessionNonce = long("sessionNonce")

        override val primaryKey = PrimaryKey(deviceID)

        fun fromDeviceID(deviceID: String): APNDevice? {
            return transaction {
                val deviceRow = APNDevice.select { APNDevice.deviceID eq deviceID }.firstOrNull()
                return@transaction deviceRow?.let { APNDevice(it) }
            }
        }

        fun fromUser(userID: tUserID): List<APNDevice> {
            return transaction {
                val result = APNDevice.select { APNDevice.userID eq userID }
                return@transaction result.map { APNDevice(it) }
            }
        }
    }
}