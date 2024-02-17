package net.mt32.expoll.entities

import net.mt32.expoll.config
import net.mt32.expoll.database.DatabaseEntity
import net.mt32.expoll.database.UUIDLength
import net.mt32.expoll.helper.UnixTimestamp
import net.mt32.expoll.helper.toUnixTimestampFromDB
import net.mt32.expoll.helper.upsertCustom
import net.mt32.expoll.tUserID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class UserDeletionConfirmation : DatabaseEntity {
    val userID: tUserID
    val initTimestamp: UnixTimestamp
    val key: String

    constructor(userID: tUserID) {
        this.userID = userID
        this.initTimestamp = UnixTimestamp.now()
        this.key = createKey()
    }

    constructor(resultRow: ResultRow) {
        this.userID = resultRow[UserDeletionConfirmation.userID]
        this.initTimestamp = resultRow[UserDeletionConfirmation.initTimestamp].toUnixTimestampFromDB()
        this.key = resultRow[UserDeletionConfirmation.key]
    }

    companion object : Table("userDeletionConfirmation") {
        val userID = varchar("userID", UUIDLength)
        val initTimestamp = long("initTimestamp")
        val key = varchar("key", 255)

        override val primaryKey = PrimaryKey(userID)

        private fun createKey(): String {
            return UUID.randomUUID().toString()
        }

        fun getPendingConfirmationForKey(key: String): UserDeletionConfirmation? {
            return transaction {
                val resultRow = selectAll().where { UserDeletionConfirmation.key eq key }.firstOrNull()
                return@transaction resultRow?.let { UserDeletionConfirmation(it) }
            }
        }

        fun getPendingConfirmationForUser(userID: tUserID): UserDeletionConfirmation? {
            return transaction {
                val resultRow =
                    selectAll().where { UserDeletionConfirmation.userID eq userID }.firstOrNull()
                return@transaction resultRow?.let { UserDeletionConfirmation(it) }
            }
        }
    }

    override fun save(): Boolean {
        transaction {
            UserDeletionConfirmation.upsertCustom(UserDeletionConfirmation.userID) {
                it[userID] = this@UserDeletionConfirmation.userID
                it[initTimestamp] = this@UserDeletionConfirmation.initTimestamp.toDB()
                it[key] = this@UserDeletionConfirmation.key
            }
        }
        return true
    }

    override fun delete(): Boolean {
        transaction {
            UserDeletionConfirmation.deleteWhere { UserDeletionConfirmation.userID eq userID }
        }
        return true
    }
}

class UserDeletionQueue : DatabaseEntity {
    enum class DeletionStage(val value: Int) {
        DEACTIVATION(0),
        DELETION(1),
        FINAL_DELETION(2); // not used currently, state after deletion

        companion object {
            fun fromValue(value: Int): DeletionStage {
                return when (value) {
                    0 -> DEACTIVATION
                    1 -> DELETION
                    2 -> FINAL_DELETION
                    else -> throw IllegalArgumentException("Invalid DeletionStage value")
                }
            }
        }
    }

    val userID: tUserID
    var nextDeletionDate: UnixTimestamp
    var currentDeletionStage: DeletionStage

    val user: User
        get() = User.loadFromID(userID)!!

    constructor(userID: tUserID, deletionDate: UnixTimestamp, deletionStage: DeletionStage) {
        this.userID = userID
        this.nextDeletionDate = deletionDate
        this.currentDeletionStage = deletionStage
    }

    constructor(resultRow: ResultRow) {
        this.userID = resultRow[UserDeletionQueue.userID]
        this.nextDeletionDate = resultRow[UserDeletionQueue.nextDeletionDate].toUnixTimestampFromDB()
        this.currentDeletionStage = DeletionStage.fromValue(resultRow[UserDeletionQueue.currentDeletionStage])
    }

    override fun save(): Boolean {
        transaction {
            UserDeletionQueue.upsertCustom(UserDeletionQueue.userID) {
                it[userID] = this@UserDeletionQueue.userID
                it[nextDeletionDate] = this@UserDeletionQueue.nextDeletionDate.toDB()
                it[currentDeletionStage] = this@UserDeletionQueue.currentDeletionStage.value
            }
        }
        return true
    }

    override fun delete(): Boolean {
        transaction {
            UserDeletionQueue.deleteWhere { UserDeletionQueue.userID eq userID }
        }
        return true
    }

    companion object : Table("userDeletionQueue") {
        val userID = varchar("userID", UUIDLength)
        val nextDeletionDate = long("nextDeletionDate")
        val currentDeletionStage = integer("currentDeletionStage")

        override val primaryKey = PrimaryKey(userID)

        fun getPendingDeletionForUser(userID: tUserID): UserDeletionQueue? {
            return transaction {
                val resultRow = selectAll().where { UserDeletionQueue.userID eq userID }.firstOrNull()
                return@transaction resultRow?.let { UserDeletionQueue(it) }
            }
        }

        fun userIsInDeletionQueue(userID: tUserID): Boolean {
            return transaction { selectAll().where { UserDeletionQueue.userID eq userID }.firstOrNull() != null }
        }

        private fun getNextDeletionDateForNextStage(stage: DeletionStage): UnixTimestamp {
            return when (stage) {
                DeletionStage.DEACTIVATION -> UnixTimestamp.now()
                    .addDays(config.dataRetention.userDeleteAfterAdditionalDays)

                DeletionStage.DELETION -> UnixTimestamp.now().addDays(config.dataRetention.userDeletionFinalAfterDays)
                DeletionStage.FINAL_DELETION -> UnixTimestamp.now()
            }
        }

        fun deactivateUser(userID: tUserID) {
            UserDeletionQueue(userID, getNextDeletionDateForNextStage(DeletionStage.DEACTIVATION), DeletionStage.DEACTIVATION).save()
        }

        fun addUserToDeletionQueueOrPropagate(
            userID: tUserID,
            assumedCurrentStage: DeletionStage? = null,
            forcedNextDeletionDate: UnixTimestamp? = null
        ) {
            if (userIsInDeletionQueue(userID)) {
                val userDeletionQueue = getPendingDeletionForUser(userID)!!
                val currentStage = assumedCurrentStage ?: userDeletionQueue.currentDeletionStage
                when (currentStage) {
                    DeletionStage.DEACTIVATION -> {
                        val nextDeletionDate =
                            UnixTimestamp.now().addDays(config.dataRetention.userDeleteAfterAdditionalDays)
                        userDeletionQueue.nextDeletionDate = forcedNextDeletionDate ?: nextDeletionDate
                        userDeletionQueue.currentDeletionStage = DeletionStage.DELETION
                    }

                    DeletionStage.DELETION -> {
                        val nextDeletionDate =
                            UnixTimestamp.now().addDays(config.dataRetention.userDeleteAfterAdditionalDays)
                        userDeletionQueue.nextDeletionDate = forcedNextDeletionDate ?: nextDeletionDate
                        userDeletionQueue.currentDeletionStage = DeletionStage.FINAL_DELETION
                    }

                    DeletionStage.FINAL_DELETION -> {} // do nothing
                }
                userDeletionQueue.save()
                return
            }
            val nextDeletionDate = UnixTimestamp.now().addDays(config.dataRetention.userDeleteAfterAdditionalDays)
            UserDeletionQueue(
                userID,
                forcedNextDeletionDate ?: nextDeletionDate,
                assumedCurrentStage ?: DeletionStage.DEACTIVATION
            ).save()
        }

        fun removeUserFromDeletionQueue(userID: tUserID) {
            transaction {
                UserDeletionQueue.deleteWhere { UserDeletionQueue.userID eq userID }
            }
        }

        fun getDeletionQueue(): List<UserDeletionQueue> {
            return transaction {
                val resultRows = selectAll().orderBy(UserDeletionQueue.nextDeletionDate).toList()
                return@transaction resultRows.map { UserDeletionQueue(it) }
            }
        }

        fun getDeletionQueueForStage(stage: DeletionStage): List<UserDeletionQueue> {
            return transaction {
                val resultRows = selectAll().where { UserDeletionQueue.currentDeletionStage eq stage.value }
                .orderBy(UserDeletionQueue.nextDeletionDate).toList()
                return@transaction resultRows.map { UserDeletionQueue(it) }
            }
        }

        fun noteFinalDeletion(userID: tUserID) {
            UserDeletionQueue(userID, UnixTimestamp.now(), DeletionStage.FINAL_DELETION).save()
        }
    }
}