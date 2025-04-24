package net.mt32.expoll.entities.notifications

import net.mt32.expoll.database.DatabaseEntity
import net.mt32.expoll.database.UUIDLength
import net.mt32.expoll.entities.Session
import net.mt32.expoll.commons.helper.UnixTimestamp
import net.mt32.expoll.commons.helper.toUnixTimestampFromDB
import net.mt32.expoll.helper.upsertCustom
import net.mt32.expoll.notification.APNsNotificationHandler
import net.mt32.expoll.notification.UniversalNotification
import net.mt32.expoll.commons.tUserID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction


class APNDevice : DatabaseEntity, NotificationDevice {
    val deviceID: String
    var userID: tUserID
    override val creationTimestamp: UnixTimestamp
    override val session: Session?
        get() = Session.fromNonce(sessionNonce)

    override var sessionNonce: Long

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
        this.deviceID = apnDeviceRow[Companion.deviceID]
        this.userID = apnDeviceRow[Companion.userID]
        this.creationTimestamp = apnDeviceRow[Companion.creationTimestamp].toUnixTimestampFromDB()
        this.sessionNonce = apnDeviceRow[Companion.sessionNonce]
    }

    override fun save(): Boolean {
        transaction {
            APNDevice.upsertCustom(Companion.deviceID) {
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
                deviceID eq this@APNDevice.deviceID
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
                val deviceRow = APNDevice.selectAll().where { Companion.deviceID eq deviceID }.firstOrNull()
                return@transaction deviceRow?.let { APNDevice(it) }
            }
        }

        fun fromUser(userID: tUserID): List<APNDevice> {
            return transaction {
                val result = APNDevice.selectAll().where { Companion.userID eq userID }
                return@transaction result.map { APNDevice(it) }
            }
        }

        fun all(): List<APNDevice> {
            return transaction {
                val result = APNDevice.selectAll()
                return@transaction result.map { APNDevice(it) }
            }
        }
    }

    override fun sendNotification(universalNotification: UniversalNotification) {
        APNsNotificationHandler.sendNotification(universalNotification, this)
    }
}

