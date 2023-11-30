package net.mt32.expoll.entities

import com.yubico.webauthn.data.ByteArray
import io.ktor.util.*
import kotlinx.serialization.Serializable
import net.mt32.expoll.config
import net.mt32.expoll.database.DatabaseEntity
import net.mt32.expoll.database.UUIDLength
import net.mt32.expoll.entities.notifications.APNDevice
import net.mt32.expoll.entities.notifications.WebNotificationDevice
import net.mt32.expoll.helper.UnixTimestamp
import net.mt32.expoll.helper.toUnixTimestampFromDB
import net.mt32.expoll.helper.upsertCustom
import net.mt32.expoll.serializable.admin.responses.UserInfo
import net.mt32.expoll.serializable.request.SortingOrder
import net.mt32.expoll.serializable.responses.SimpleUser
import net.mt32.expoll.tPollID
import net.mt32.expoll.tUserID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.reflect.full.memberProperties

interface ISimpleUser {
    val firstName: String
    val lastName: String
    val username: String
    val id: String
}

interface IUser : ISimpleUser {
    override val id: tUserID
    override var username: String
    override var firstName: String
    override var lastName: String
    var mail: String
    val polls: List<Poll>
    val votes: List<Vote>
    val sessions: List<Session>
    val notes: List<PollUserNote>
    var active: Boolean
    var admin: Boolean
    val challenges: List<Challenge>
    val authenticators: List<Authenticator>
}

class User : IUser, DatabaseEntity {
    override var id: String
        private set
    override var username: String
    override var firstName: String
    override var lastName: String
    val fullName: String
        get() = "$firstName $lastName"

    override var mail: String
    override val polls: List<Poll>
        get() {
            return cachedPolls ?: Poll.accessibleForUser(id)
        }
    override val votes: List<Vote>
        get() = Vote.fromUser(this)

    val otps: List<OTP>
        get() = OTP.fromUser(id)

    override val sessions: List<Session>
        get() {
            return Session.forUser(id)
        }
    override val notes: List<PollUserNote>
        get() {
            return PollUserNote.forUser(id)
        }
    override var active: Boolean
    override var admin: Boolean

    val superAdmin: Boolean
        get() = mail.equals(config.superAdminMail, ignoreCase = true)
    override val challenges: List<Challenge>
        get() {
            return Challenge.forUser(id)
        }
    override val authenticators: List<Authenticator>
        get() {
            return Authenticator.fromUser(id)
        }

    private var cachedPolls: List<Poll>? = null

    val notificationPreferences: NotificationPreferences
        get() = NotificationPreferences.fromUser(id)

    val apnDevices: List<APNDevice>
        get() = APNDevice.fromUser(id)

    val webNotificationDevices: List<WebNotificationDevice>
        get() = WebNotificationDevice.fromUser(id)

    val created: UnixTimestamp
    val deleted: UnixTimestamp?

    val oidConnections: List<OIDCUserData>
        get() = OIDCUserData.byUser(id)


    constructor(
        username: String,
        firstName: String,
        lastName: String,
        mail: String,
        active: Boolean = true,
        admin: Boolean
    ) {
        this.id = UUID.randomUUID().toString()
        this.username = username
        this.firstName = firstName
        this.lastName = lastName
        this.mail = mail
        this.active = active
        this.admin = admin
        this.created = UnixTimestamp.now()
        this.deleted = null
    }

    constructor(userRow: ResultRow) {
        this.id = userRow[User.id]
        this.username = userRow[User.username]
        this.mail = userRow[User.mail]
        this.firstName = userRow[User.firstName]
        this.lastName = userRow[User.lastName]
        this.active = userRow[User.active]
        this.admin = userRow[User.admin] || config.superAdminMail.equals(mail, ignoreCase = true)
        this.created = userRow[User.created].toUnixTimestampFromDB()
        this.deleted = userRow[User.deleted]?.toUnixTimestampFromDB()
    }

    override fun save(): Boolean {
        transaction {
            User.upsertCustom(User.id) {
                it[id] = this@User.id
                it[username] = this@User.username
                it[mail] = this@User.mail
                it[firstName] = this@User.firstName
                it[lastName] = this@User.lastName
                it[active] = this@User.active
                it[admin] = this@User.admin
                it[created] = this@User.created.toDB()
                it[deleted] = this@User.deleted?.toDB()
            }
        }
        return true
    }

    override fun saveConsecutive(): Boolean {
        save()
        transaction {
            sessions.forEach { it.save() }
            challenges.forEach { it.save() }
            authenticators.forEach { it.save() }
            votes.forEach { it.save() }
        }
        return true
    }

    override fun delete(): Boolean {
        otps.forEach { it.delete() }
        sessions.forEach { it.delete() }
        challenges.forEach { it.delete() }
        authenticators.forEach { it.delete() }
        apnDevices.forEach { it.delete() }
        OIDCUserData.byUser(id).forEach { it.delete() }
        UserDeletionConfirmation.getPendingConfirmationForUser(id)?.delete()
        webNotificationDevices.forEach { it.delete() }
        //votes.forEach { it.delete() }
        //polls.forEach { if(it.adminID != id) UserPolls.removeConnection(id, it.id) }
        val oldActive = active
        transaction {
            User.upsertCustom(User.id) {
                it[id] = this@User.id
                it[username] = "Deleted User " + this@User.id
                it[firstName] = "Deleted"
                it[lastName] = "User"
                it[mail] = "unknown" + this@User.id
                it[created] = this@User.created.toDB()
                it[active] = false
                it[admin] = false
                it[deleted] = UnixTimestamp.now().toDB()
            }
        }
        if (!oldActive) {
            votes.forEach { it.delete() }
            notes.forEach { it.delete() }
            polls.forEach {
                if (it.adminID == id) it.delete()
                UserPolls.removeConnection(id, it.id)
            }
            notificationPreferences.delete()
            transaction {
                User.deleteWhere { User.id eq this@User.id }
            }
        }
        return true
    }

    /**
     * Creates and saves new OTP for current user
     */
    fun createOTP(forApp: Boolean): OTP {
        return OTP.create(id, forApp)
    }

    fun createSessionFromScratch(): Session {
        val session = Session(id, "unknown")
        session.save()
        return session
    }

    companion object : Table("user") {
        const val maxUserNameLength = 255
        const val maxNameComponentLength = 255
        const val maxMailLength = 255


        val id = varchar("id", UUIDLength).default(UUID.randomUUID().toString())
        val username = varchar("username", maxUserNameLength).uniqueIndex()
        val firstName = varchar("firstName", maxNameComponentLength)
        val lastName = varchar("lastName", maxNameComponentLength)
        val mail = varchar("mail", maxMailLength).uniqueIndex()
        val active = bool("active")
        val admin = bool("admin")
        val created = long("createdTimestamp")
        val deleted = long("deletedTimestamp").nullable()


        override val primaryKey = PrimaryKey(id)

        fun loadFromID(id: String): User? {
            return transaction {
                val userRow = User.select { User.id eq id }.limit(1).firstOrNull()
                return@transaction userRow?.let { User(it) }
            }
        }

        fun byMail(mail: String): User? {
            return transaction {
                val userRow = User.select { User.mail eq mail }.firstOrNull()
                return@transaction userRow?.let { User(it) }
            }
        }

        fun byUsername(username: String): User? {
            return transaction {
                val userRow = User.select { User.username eq username }.firstOrNull()
                return@transaction userRow?.let { User(it) }
            }
        }

        fun all(): List<User> {
            return transaction {
                return@transaction User.selectAll().toList().map { User(it) }
            }
        }

        fun all(limit: Int, offset: Long, searchParameters: UserSearchParameters? = null): List<User> {
            return transaction {
                val query = if (searchParameters == null) User.selectAll()
                else User.select {
                    val specialFilter = when (searchParameters.specialFilter) {
                        UserSearchParameters.SpecialFilter.ALL -> Op.TRUE
                        UserSearchParameters.SpecialFilter.DELETED -> User.deleted.isNotNull()
                        UserSearchParameters.SpecialFilter.OIDC -> User.id inSubQuery OIDCUserData.select { OIDCUserData.userID eq User.id }
                            .adjustSlice { slice(OIDCUserData.userID) }

                        UserSearchParameters.SpecialFilter.ADMIN -> User.admin
                    }

                    val username =
                        (if (searchParameters.searchQuery.username != null) (User.username like "%${searchParameters.searchQuery.username}%") else Op.TRUE)
                    val firstName =
                        (if (searchParameters.searchQuery.firstName != null) (User.firstName like "%${searchParameters.searchQuery.firstName}%") else Op.TRUE)
                    val lastName =
                        (if (searchParameters.searchQuery.lastName != null) (User.lastName like "%${searchParameters.searchQuery.lastName}%") else Op.TRUE)
                    val memberInPoll =
                        (if (searchParameters.searchQuery.memberInPoll != null) (User.id inSubQuery UserPolls.select { UserPolls.pollID like "%${searchParameters.searchQuery.memberInPoll}%" }
                            .adjustSlice { slice(UserPolls.userID) }) else Op.TRUE)
                    val any = (if (searchParameters.searchQuery.any != null)
                        ((User.username like "%${searchParameters.searchQuery.any}%") or
                                (User.firstName like "%${searchParameters.searchQuery.any}%") or
                                (User.lastName like "%${searchParameters.searchQuery.any}%")
                                ) else Op.TRUE)

                    val query = username and firstName and lastName and memberInPoll and any

                    return@select query and specialFilter

                }
                val sorted = query.orderBy(
                    when (searchParameters?.sortingStrategy) {
                        UserSearchParameters.SortingStrategy.USERNAME -> username
                        UserSearchParameters.SortingStrategy.FIRST_NAME -> firstName
                        UserSearchParameters.SortingStrategy.LAST_NAME -> lastName
                        UserSearchParameters.SortingStrategy.MAIL -> mail
                        UserSearchParameters.SortingStrategy.CREATED -> created
                        UserSearchParameters.SortingStrategy.DELETED -> deleted
                        null -> created
                    } to
                            when (searchParameters?.sortingOrder) {
                                SortingOrder.ASCENDING -> SortOrder.ASC
                                SortingOrder.DESCENDING -> SortOrder.DESC
                                null -> SortOrder.ASC
                            }
                )
                return@transaction sorted.limit(limit, offset).toList().map { User(it) }
            }
        }

        fun fromUserHandle(handle: ByteArray?): User? {
            if (handle == null) return null
            val b64 = handle.base64
            val decoded = b64.decodeBase64String()
            return loadFromID(decoded)
        }

        fun ensureTestUserExistence() {
            val existing = User.byUsername(config.testUser.username)
            if (existing != null) return
            val user = User(
                config.testUser.username,
                config.testUser.firstName,
                config.testUser.lastName,
                config.testUser.email,
                admin = false
            )
            user.save()
        }

        fun admins(): List<User> {
            return transaction {
                return@transaction User.select { (User.admin eq true) or (User.mail eq config.superAdminMail) }
                    .map { User(it) }
            }
        }

        /*val id = "4411a4b1-f62a-11ec-bd56-0242ac190002"
    val b64 = id.encodeBase64()
    println(b64)
    val bb64 = ByteArray.fromBase64(b64)
    val decoded2 = bb64.base64
    println(decoded2.decodeBase64String())
    println(b64.decodeBase64String())*/
    }

    val userHandle: ByteArray
        get() = ByteArray.fromBase64(id.encodeBase64())

    fun asSimpleUser(): SimpleUser {
        return SimpleUser(
            firstName,
            lastName,
            username,
            id
        )
    }

    fun asUserInfo(): UserInfo {
        return UserInfo(
            id,
            username,
            firstName,
            lastName,
            mail,
            admin,
            superAdmin,
            active,
            oidConnections.map { it.toConnectionOverview().name },
            created.toClient()
        )
    }

    override fun equals(other: Any?): Boolean {
        if (other is User) return this.id == other.id
        if (other is SimpleUser) return this.id == other.id
        return super.equals(other)
    }

    fun addPoll(pollID: tPollID) {
        UserPolls.addConnection(id, pollID)
    }

    fun removePoll(pollID: tPollID) {
        UserPolls.removeConnection(id, pollID)
    }

}

@Serializable
data class UserSearchParameters(
    val sortingOrder: SortingOrder = SortingOrder.DESCENDING,
    val sortingStrategy: SortingStrategy = SortingStrategy.CREATED,
    val specialFilter: SpecialFilter = SpecialFilter.ALL,
    val searchQuery: Query = Query()
) {
    enum class SortingStrategy {
        CREATED,
        USERNAME,
        FIRST_NAME,
        LAST_NAME,
        MAIL,
        DELETED
    }

    enum class SpecialFilter {
        ALL,
        DELETED,
        OIDC,
        ADMIN
    }

    @Serializable
    data class Query(
        val username: String? = null,
        val firstName: String? = null,
        val lastName: String? = null,
        val memberInPoll: tPollID? = null,
        val any: String? = null
    )

    @Serializable
    data class Descriptor(
        val sortingOrder: List<SortingOrder> = SortingOrder.values().toList(),
        val sortingStrategy: List<SortingStrategy> = SortingStrategy.values().toList(),
        val specialFilter: List<SpecialFilter> = SpecialFilter.values().toList(),
        val searchQuery: List<String> = Query::class.memberProperties.map { it.name }
    )
}

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
                val resultRow = UserDeletionConfirmation.select { UserDeletionConfirmation.key eq key }.firstOrNull()
                return@transaction resultRow?.let { UserDeletionConfirmation(it) }
            }
        }

        fun getPendingConfirmationForUser(userID: tUserID): UserDeletionConfirmation? {
            return transaction {
                val resultRow =
                    UserDeletionConfirmation.select { UserDeletionConfirmation.userID eq userID }.firstOrNull()
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