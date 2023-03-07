package net.mt32.expoll.entities

import net.mt32.expoll.database.DatabaseEntity
import net.mt32.expoll.database.UUIDLength
import net.mt32.expoll.tUserID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction


class APNDevice : DatabaseEntity {
    val deviceID: String
    val userID: tUserID
    val creationTimestamp: Long

    constructor(deviceID: String, userID: tUserID, creationTimestamp: Long) {
        this.deviceID = deviceID
        this.userID = userID
        this.creationTimestamp = creationTimestamp
    }

    private constructor(apnDeviceRow: ResultRow) {
        this.deviceID = apnDeviceRow[APNDevice.deviceID]
        this.userID = apnDeviceRow[APNDevice.userID]
        this.creationTimestamp = apnDeviceRow[APNDevice.creationTimestamp]
    }

    override fun save() {
        TODO("not implemented yet")
    }

    companion object : Table("apn_devices") {
        val deviceID = varchar("deviceId", 255)
        val userID = varchar("userId", UUIDLength)
        val creationTimestamp = long("creationTimestamp")

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