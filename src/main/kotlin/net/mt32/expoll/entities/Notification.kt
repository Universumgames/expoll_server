package net.mt32.expoll.entities

import kotlinx.serialization.Serializable
import net.mt32.expoll.database.DatabaseEntity
import net.mt32.expoll.database.UUIDLength
import net.mt32.expoll.helper.upsertCustom
import net.mt32.expoll.commons.tUserID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

@Serializable
data class NotificationPreferencesSerial(
    val voteChange: Boolean? = null,
    val voteChangeDetailed: Boolean? = null,
    val userAdded: Boolean? = null,
    val userRemoved: Boolean? = null,
    val pollDeleted: Boolean? = null,
    val pollEdited: Boolean? = null,
    val pollArchived: Boolean? = null,
    val newLogin: Boolean? = null
)

class NotificationPreferences : DatabaseEntity {
    val id: String
    val userID: tUserID
    var voteChange: Boolean
    var voteChangeDetailed: Boolean
    var userAdded: Boolean
    var userRemoved: Boolean
    var pollDeleted: Boolean
    var pollEdited: Boolean
    var pollArchived: Boolean
    var newLogin: Boolean

    constructor(
        id: String,
        userID: tUserID,
        voteChange: Boolean,
        voteChangeDetailed: Boolean,
        userAdded: Boolean,
        userRemoved: Boolean,
        pollDeleted: Boolean,
        pollEdited: Boolean,
        pollArchived: Boolean,
        newLogin: Boolean
    ) {
        this.id = id
        this.userID = userID
        this.voteChange = voteChange
        this.voteChangeDetailed = voteChangeDetailed
        this.userAdded = userAdded
        this.userRemoved = userRemoved
        this.pollDeleted = pollDeleted
        this.pollEdited = pollEdited
        this.pollArchived = pollArchived
        this.newLogin = newLogin
    }

    constructor(userID: tUserID) {
        var uuid = UUID.randomUUID().toString()
        while (fromUUID(uuid) != null) {
            uuid = UUID.randomUUID().toString()
        }
        this.id = uuid
        this.userID = userID
        this.voteChange = true
        this.voteChangeDetailed = false
        this.userAdded = true
        this.userRemoved = true
        this.pollDeleted = true
        this.pollEdited = true
        this.pollArchived = true
        this.newLogin = true
    }

    private constructor(notificationRow: ResultRow) {
        this.id = notificationRow[NotificationPreferences.id]
        this.userID = notificationRow[NotificationPreferences.userID]
        this.voteChange = notificationRow[NotificationPreferences.voteChange]
        this.voteChangeDetailed = notificationRow[NotificationPreferences.voteChangeDetailed]
        this.userAdded = notificationRow[NotificationPreferences.userAdded]
        this.userRemoved = notificationRow[NotificationPreferences.userRemoved]
        this.pollDeleted = notificationRow[NotificationPreferences.pollDeleted]
        this.pollEdited = notificationRow[NotificationPreferences.pollEdited]
        this.pollArchived = notificationRow[NotificationPreferences.pollArchived]
        this.newLogin = notificationRow[NotificationPreferences.newLogin]
    }

    override fun save(): Boolean {
        transaction {
            NotificationPreferences.upsertCustom(NotificationPreferences.id) {
                it[id] = this@NotificationPreferences.id
                it[userID] = this@NotificationPreferences.userID
                it[voteChange] = this@NotificationPreferences.voteChange
                it[voteChangeDetailed] = this@NotificationPreferences.voteChangeDetailed
                it[userAdded] = this@NotificationPreferences.userAdded
                it[userRemoved] = this@NotificationPreferences.userRemoved
                it[pollDeleted] = this@NotificationPreferences.pollDeleted
                it[pollEdited] = this@NotificationPreferences.pollEdited
                it[pollArchived] = this@NotificationPreferences.pollArchived
                it[newLogin] = this@NotificationPreferences.newLogin
            }
        }
        return true
    }

    override fun delete(): Boolean {
        transaction {
            NotificationPreferences.deleteWhere {
                id eq this@NotificationPreferences.id
            }
        }
        return true
    }

    fun toSerializable(): NotificationPreferencesSerial {
        return NotificationPreferencesSerial(
            voteChange = voteChange,
            voteChangeDetailed = voteChangeDetailed,
            userAdded = userAdded,
            userRemoved = userRemoved,
            pollDeleted = pollDeleted,
            pollEdited = pollEdited,
            pollArchived = pollArchived,
            newLogin = newLogin
        )
    }

    companion object : Table("notification_preferences_entity") {
        val id = varchar("id", UUIDLength)
        val userID = varchar("userId", UUIDLength).uniqueIndex()
        val voteChange = bool("voteChange")
        val voteChangeDetailed = bool("voteChangeDetailed")
        val userAdded = bool("userAdded")
        val userRemoved = bool("userRemoved")
        val pollDeleted = bool("pollDeleted")
        val pollEdited = bool("pollEdited")
        val pollArchived = bool("pollArchived")
        val newLogin = bool("newLogin")

        override val primaryKey = PrimaryKey(id)

        fun fromUser(userID: tUserID): NotificationPreferences {
            return transaction {
                val notificationRow =
                    NotificationPreferences.selectAll().where { NotificationPreferences.userID eq userID }.firstOrNull()
                return@transaction notificationRow?.let { NotificationPreferences(it) }
            } ?: NotificationPreferences(userID)
        }

        fun fromUUID(uuid: String): NotificationPreferences? {
            return transaction {
                val notificationRow =
                    NotificationPreferences.selectAll().where { NotificationPreferences.userID eq uuid }.firstOrNull()
                return@transaction notificationRow?.let { NotificationPreferences(it) }
            }
        }
    }
}