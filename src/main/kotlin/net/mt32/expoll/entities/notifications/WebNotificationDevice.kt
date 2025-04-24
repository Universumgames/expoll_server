package net.mt32.expoll.entities.notifications

import net.mt32.expoll.database.DatabaseEntity
import net.mt32.expoll.database.UUIDLength
import net.mt32.expoll.entities.Session
import net.mt32.expoll.commons.helper.UnixTimestamp
import net.mt32.expoll.commons.helper.toUnixTimestampFromDB
import net.mt32.expoll.helper.upsertCustom
import net.mt32.expoll.notification.UniversalNotification
import net.mt32.expoll.notification.WebNotificationHandler
import net.mt32.expoll.commons.tUserID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class WebNotificationDevice : DatabaseEntity, NotificationDevice {

    val endpoint: String
    var auth: String
    var p256dh: String
    val userID: tUserID
    val expirationTimestamp: UnixTimestamp?
    override val creationTimestamp: UnixTimestamp
    override val sessionNonce: Long

    override val session: Session?
        get() = Session.fromNonce(sessionNonce)

    constructor(
        endpoint: String,
        auth: String,
        p256dh: String,
        userID: tUserID,
        expirationTimestamp: UnixTimestamp? = null,
        creationTimestamp: UnixTimestamp = UnixTimestamp.now(),
        sessionNonce: Long
    ) {
        this.endpoint = endpoint
        this.auth = auth
        this.p256dh = p256dh
        this.userID = userID
        this.expirationTimestamp = expirationTimestamp
        this.creationTimestamp = creationTimestamp
        this.sessionNonce = sessionNonce
    }

    constructor(it: ResultRow) {
        this.endpoint = it[Companion.endpoint]
        this.auth = it[Companion.auth]
        this.p256dh = it[Companion.p256dh]
        this.userID = it[Companion.userID]
        this.expirationTimestamp = it[Companion.expirationTimestamp]?.toUnixTimestampFromDB()
        this.creationTimestamp = it[Companion.creationTimestamp].toUnixTimestampFromDB()
        this.sessionNonce = it[Companion.sessionNonce]
    }

    override fun save(): Boolean {
        transaction {
            WebNotificationDevice.upsertCustom(Companion.endpoint) {
                it[endpoint] = this@WebNotificationDevice.endpoint
                it[auth] = this@WebNotificationDevice.auth
                it[p256dh] = this@WebNotificationDevice.p256dh
                it[userID] = this@WebNotificationDevice.userID
                it[expirationTimestamp] = this@WebNotificationDevice.expirationTimestamp?.toDB()
                it[creationTimestamp] = this@WebNotificationDevice.creationTimestamp.toDB()
                it[sessionNonce] = this@WebNotificationDevice.sessionNonce
            }
        }
        return true
    }

    override fun delete(): Boolean {
        transaction {
            WebNotificationDevice.deleteWhere {
                endpoint eq this@WebNotificationDevice.endpoint
            }
        }
        return true
    }

    companion object : Table("web_notification_devices") {
        val endpoint = varchar("endpoint", 255)
        val auth = varchar("auth", 255)
        val p256dh = varchar("p256dh", 255)
        val userID = varchar("userId", UUIDLength)
        val expirationTimestamp = long("expirationTimestamp").nullable()
        val creationTimestamp = long("creationTimestamp")
        val sessionNonce = long("sessionNonce")

        override val primaryKey = PrimaryKey(endpoint)

        fun fromEndpoint(endpoint: String): WebNotificationDevice? {
            return transaction {
                val deviceRow =
                    WebNotificationDevice.selectAll().where { Companion.endpoint eq endpoint }.firstOrNull()
                return@transaction deviceRow?.let { WebNotificationDevice(it) }
            }
        }

        fun fromUser(userID: tUserID): List<WebNotificationDevice> {
            return transaction {
                val result = WebNotificationDevice.selectAll().where { Companion.userID eq userID }
                return@transaction result.map { WebNotificationDevice(it) }
            }
        }

        fun all(): List<WebNotificationDevice> {
            return transaction {
                val result = WebNotificationDevice.selectAll()
                return@transaction result.map { WebNotificationDevice(it) }
            }
        }
    }

    override fun sendNotification(universalNotification: UniversalNotification) {
        WebNotificationHandler.sendNotification(universalNotification, this)
    }
}