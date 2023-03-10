package net.mt32.expoll.entities

import kotlinx.serialization.Serializable
import net.mt32.expoll.database.DatabaseEntity
import net.mt32.expoll.database.UUIDLength
import net.mt32.expoll.helper.upsert
import net.mt32.expoll.tUserID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*


@Serializable
class NotificationPreferences : DatabaseEntity {
    val id: String
    val userID: tUserID
    var voteChange: Boolean
    var userAdded: Boolean
    var userRemoved: Boolean
    var pollDeleted: Boolean
    var pollEdited: Boolean
    var pollArchived: Boolean

    constructor(
        id: String,
        userID: tUserID,
        voteChange: Boolean,
        userAdded: Boolean,
        userRemoved: Boolean,
        pollDeleted: Boolean,
        pollEdited: Boolean,
        pollArchived: Boolean
    ) {
        this.id = id
        this.userID = userID
        this.voteChange = voteChange
        this.userAdded = userAdded
        this.userRemoved = userRemoved
        this.pollDeleted = pollDeleted
        this.pollEdited = pollEdited
        this.pollArchived = pollArchived
    }

    constructor(userID: tUserID) {
        var uuid = UUID.randomUUID().toString()
        while (fromUUID(uuid) != null) {
            uuid = UUID.randomUUID().toString()
        }
        this.id = uuid
        this.userID = userID
        this.voteChange = true
        this.userAdded = true
        this.userRemoved = true
        this.pollDeleted = true
        this.pollEdited = true
        this.pollArchived = true
    }

    private constructor(notificationRow: ResultRow) {
        this.id = notificationRow[NotificationPreferences.id]
        this.userID = notificationRow[NotificationPreferences.userID]
        this.voteChange = notificationRow[NotificationPreferences.voteChange]
        this.userAdded = notificationRow[NotificationPreferences.userAdded]
        this.userRemoved = notificationRow[NotificationPreferences.userRemoved]
        this.pollDeleted = notificationRow[NotificationPreferences.pollDeleted]
        this.pollEdited = notificationRow[NotificationPreferences.pollEdited]
        this.pollArchived = notificationRow[NotificationPreferences.pollArchived]
    }

    override fun save() {
        transaction {
            NotificationPreferences.upsert(NotificationPreferences.id) {
                it[id] = this@NotificationPreferences.id
                it[userID] = this@NotificationPreferences.userID
                it[voteChange] = this@NotificationPreferences.voteChange
                it[userAdded] = this@NotificationPreferences.userAdded
                it[userRemoved] = this@NotificationPreferences.userRemoved
                it[pollDeleted] = this@NotificationPreferences.pollDeleted
                it[pollEdited] = this@NotificationPreferences.pollEdited
                it[pollArchived] = this@NotificationPreferences.pollArchived
            }
        }
    }

    companion object : Table("notification_preferences_entity") {
        val id = varchar("id", UUIDLength)
        val userID = varchar("userId", UUIDLength).uniqueIndex()
        val voteChange = bool("voteChange")
        val userAdded = bool("userAdded")
        val userRemoved = bool("userRemoved")
        val pollDeleted = bool("pollDeleted")
        val pollEdited = bool("pollEdited")
        val pollArchived = bool("pollArchoved")

        override val primaryKey = PrimaryKey(id)

        fun fromUser(userID: tUserID): NotificationPreferences {
            return transaction {
                val notificationRow =
                    NotificationPreferences.select { NotificationPreferences.userID eq userID }.firstOrNull()
                return@transaction notificationRow?.let { NotificationPreferences(it) }
            } ?: NotificationPreferences(userID)
        }

        fun fromUUID(uuid: String): NotificationPreferences? {
            return transaction {
                val notificationRow =
                    NotificationPreferences.select { NotificationPreferences.userID eq uuid }.firstOrNull()
                return@transaction notificationRow?.let { NotificationPreferences(it) }
            }
        }
    }
}