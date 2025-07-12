package net.mt32.expoll.entities

import com.yubico.webauthn.data.ByteArray
import io.ktor.server.application.*
import io.ktor.util.*
import net.mt32.expoll.ExpollMail
import net.mt32.expoll.Mail
import net.mt32.expoll.commons.helper.UnixTimestamp
import net.mt32.expoll.commons.helper.toUnixTimestampFromDB
import net.mt32.expoll.commons.interfaces.*
import net.mt32.expoll.commons.serializable.admin.responses.UserInfo
import net.mt32.expoll.commons.serializable.request.SortingOrder
import net.mt32.expoll.commons.serializable.request.search.UserSearchParameters
import net.mt32.expoll.commons.serializable.responses.SimpleUser
import net.mt32.expoll.commons.serializable.responses.UserDataResponse
import net.mt32.expoll.commons.tPollID
import net.mt32.expoll.commons.tUserID
import net.mt32.expoll.config
import net.mt32.expoll.database.DatabaseEntity
import net.mt32.expoll.database.UUIDLength
import net.mt32.expoll.entities.interconnect.UserPolls
import net.mt32.expoll.entities.notifications.APNDevice
import net.mt32.expoll.entities.notifications.NotificationDevice
import net.mt32.expoll.entities.notifications.WebNotificationDevice
import net.mt32.expoll.helper.URLBuilder
import net.mt32.expoll.helper.generateRandomUsername
import net.mt32.expoll.helper.upsertCustom
import net.mt32.expoll.notification.ExpollNotificationHandler
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

interface IUser : ISimpleUser {
    override val id: tUserID
    override var username: String
    override var firstName: String
    override var lastName: String
    var mail: String
    val polls: List<IPoll>
    val votes: List<IVote>
    val sessions: List<ISession>
    val notes: List<IPollUserNote>
    val active: Boolean
    var admin: Boolean
    val challenges: List<Challenge>
    val authenticators: List<Authenticator>
    var maxPollsOwned: Long
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
        private set
    override var admin: Boolean

    val superAdmin: Boolean
        get() = mail.equals(config.superAdminMail, ignoreCase = true)

    val superAdminOrAdmin: Boolean
        get() = superAdmin || admin

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

    val notificationDevices: List<NotificationDevice>
        get() = apnDevices + webNotificationDevices

    val created: UnixTimestamp
    var deleted: UnixTimestamp?
    var lastLogin: UnixTimestamp

    val oidConnections: List<OIDCUserData>
        get() = OIDCUserData.byUser(id)

    val pollsOwned: Long
        get() = Poll.ownedByUserCount(id)

    override var maxPollsOwned: Long
        get() = if (admin || superAdmin) -1 else _maxPollsOwned
        set(value) {
            _maxPollsOwned = value
        }

    private var _maxPollsOwned: Long

    val loginAble: Boolean
        get() = deleted == null

    val outstandingDeletion: UserDeletionQueue?
        get() = UserDeletionQueue.getPendingDeletionForUser(id)

    var personalDataRequestCount: Int

    private constructor(
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
        this._maxPollsOwned = 10
        this.lastLogin = UnixTimestamp.now()
        this.personalDataRequestCount = 0
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
        this._maxPollsOwned = userRow[User.maxPollsOwned]
        this.lastLogin = userRow[User.lastLogin].toUnixTimestampFromDB()
        this.personalDataRequestCount = userRow[User.personalDataRequestCount]
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
                it[lastLogin] = this@User.lastLogin.toDB()
                it[maxPollsOwned] = this@User._maxPollsOwned
                it[this.personalDataRequestCount] = this@User.personalDataRequestCount
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

    private fun cleanLoginsAndNotifications() {
        otps.forEach { it.delete() }
        sessions.forEach { it.delete() }
        challenges.forEach { it.delete() }
        authenticators.forEach { it.delete() }
        OIDCUserData.byUser(id).forEach { it.delete() }
        UserDeletionConfirmation.getPendingConfirmationForUser(id)?.delete()

        apnDevices.forEach { it.delete() }
        webNotificationDevices.forEach { it.delete() }
    }

    fun anonymizeUserData() {
        if (deleted != null) return
        notifyDeletion()
        UserDeletionQueue.addUserToDeletionQueueOrPropagate(
            id,
            assumedCurrentStage = UserDeletionQueue.DeletionStage.DEACTIVATION
        )
        cleanLoginsAndNotifications()
        username = "Deleted User $id"
        firstName = "Deleted"
        lastName = "User"
        mail = "unknown$id"
        active = false
        admin = false
        deleted = UnixTimestamp.now()
        maxPollsOwned = 0
        save()
        println("Anonymized user $id")
    }

    fun finalDelete() {
        cleanLoginsAndNotifications()
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
        UserDeletionQueue.noteFinalDeletion(id)
        println("Final Deleted user $id")
    }

    @Deprecated("Use anonymizeUserData and finalDelete instead", ReplaceWith("anonymizeUserData()"))
    override fun delete(): Boolean {
        cleanLoginsAndNotifications()
        //votes.forEach { it.delete() }
        //polls.forEach { if(it.adminID != id) UserPolls.removeConnection(id, it.id) }
        //val oldActive = active
        val deletion = UserDeletionQueue.getPendingDeletionForUser(id)
        if (deletion == null || deletion.currentDeletionStage < UserDeletionQueue.DeletionStage.DELETION) {
            anonymizeUserData()
        } else {
            finalDelete()
        }
        return true
    }

    /**
     * Creates and saves new OTP for current user
     */
    fun createOTP(forApp: Boolean): OTP {
        reactivateUser()
        return OTP.create(id, forApp)
    }

    fun createSessionFromScratch(): Session {
        val session = Session(id, "unknown")
        session.save()
        ExpollNotificationHandler.sendNewLogin(this)
        return session
    }

    fun createAdminSessionFromScratch(): Session {
        val session = Session(id, "unknown")
        session.save()
        return session
    }

    fun sendOTPMail(call: ApplicationCall, forApp: Boolean = false) {
        val otp = createOTP(forApp)
        val mailData = ExpollMail.createOTPMail(this, otp, URLBuilder.buildLoginLink(call, this, otp, false))
        Mail.sendMailAsync(mailData)
    }

    fun sendUserCreationMail(scheme: String) {
        val mailData = ExpollMail.createUserCreationMail(this, scheme)
        Mail.sendMailAsync(mailData)
    }

    companion object : Table("user") {
        const val MAX_USERNAME_LENGTH = 255
        const val MAX_NAME_COMPONENT_LENGTH = 255
        const val MAX_MAIL_LENGTH = 255


        val id = varchar("id", UUIDLength).default(UUID.randomUUID().toString())
        val username = varchar("username", MAX_USERNAME_LENGTH).uniqueIndex()
        val firstName = varchar("firstName", MAX_NAME_COMPONENT_LENGTH)
        val lastName = varchar("lastName", MAX_NAME_COMPONENT_LENGTH)
        val mail = varchar("mail", MAX_MAIL_LENGTH).uniqueIndex()
        val active = bool("active")
        val admin = bool("admin")
        val created = long("createdTimestamp")
        val deleted = long("deletedTimestamp").nullable()
        val lastLogin = long("lastLogin")
        val maxPollsOwned = long("maxPollsOwned").default(10)
        val personalDataRequestCount = integer("personalDataRequestCount").default(0)


        override val primaryKey = PrimaryKey(id)

        fun loadFromID(id: String): User? {
            return transaction {
                val userRow = User.selectAll().where { User.id eq id }.limit(1).firstOrNull()
                return@transaction userRow?.let { User(it) }
            }
        }

        fun byMail(mail: String): User? {
            return transaction {
                val userRow = User.selectAll().where { User.mail eq mail }.firstOrNull()
                return@transaction userRow?.let { User(it) }
            }
        }

        fun byUsername(username: String): User? {
            return transaction {
                val userRow = User.selectAll().where { User.username eq username }.firstOrNull()
                return@transaction userRow?.let { User(it) }
            }
        }

        fun all(): List<User> {
            return transaction {
                return@transaction User.selectAll().toList().map { User(it) }
            }
        }

        fun all(limit: Int, offset: Long, searchParameters: UserSearchParameters? = null): Pair<List<User>, Long> {
            return transaction {
                val query = if (searchParameters == null) User.selectAll()
                else User.selectAll().where {
                    val specialFilter = when (searchParameters.specialFilter) {
                        UserSearchParameters.SpecialFilter.ALL -> Op.TRUE
                        UserSearchParameters.SpecialFilter.DELETED -> User.deleted.isNotNull()
                        UserSearchParameters.SpecialFilter.OIDC -> User.id inSubQuery OIDCUserData.select(OIDCUserData.userID)
                            .where { OIDCUserData.userID eq User.id }

                        UserSearchParameters.SpecialFilter.ADMIN -> User.admin
                        UserSearchParameters.SpecialFilter.DEACTIVATED -> User.active eq false
                        UserSearchParameters.SpecialFilter.WITHOUT_SESSION -> User.id notInSubQuery Session.select(
                            Session.userID
                        ).where { Session.userID eq User.id }
                    }

                    fun sqlLike(value: String?, column: Column<String>): Op<Boolean> {
                        return if (value != null) (column like "%$value%") else Op.TRUE
                    }

                    val username = sqlLike(searchParameters.searchQuery.username, User.username)
                    val firstName = sqlLike(searchParameters.searchQuery.firstName, User.firstName)
                    val lastName = sqlLike(searchParameters.searchQuery.lastName, User.lastName)
                    val mail = sqlLike(searchParameters.searchQuery.mail, User.mail)
                    val memberInPoll =
                        (if (searchParameters.searchQuery.memberInPoll != null) (User.id inSubQuery UserPolls.select(
                            UserPolls.userID
                        )
                            .where { UserPolls.pollID like "%${searchParameters.searchQuery.memberInPoll}%" }) else Op.TRUE)
                    val any = (if (searchParameters.searchQuery.any != null)
                        ((User.username like "%${searchParameters.searchQuery.any}%") or
                                (User.firstName like "%${searchParameters.searchQuery.any}%") or
                                (User.lastName like "%${searchParameters.searchQuery.any}%") or
                                (User.mail like "%${searchParameters.searchQuery.any}%")
                                ) else Op.TRUE)

                    val query = username and firstName and lastName and mail and memberInPoll and any

                    return@where query and specialFilter

                }
                val sorted = query.orderBy(
                    when (searchParameters?.sortingStrategy) {
                        UserSearchParameters.SortingStrategy.USERNAME -> username
                        UserSearchParameters.SortingStrategy.FIRST_NAME -> firstName
                        UserSearchParameters.SortingStrategy.LAST_NAME -> lastName
                        UserSearchParameters.SortingStrategy.MAIL -> mail
                        UserSearchParameters.SortingStrategy.CREATED -> created
                        UserSearchParameters.SortingStrategy.DELETED -> deleted
                        UserSearchParameters.SortingStrategy.POLLS_OWNED -> wrapAsExpression(Poll.select(Poll.adminID.count()).where { Poll.adminID eq User.id })
                        null -> created
                    } to
                            when (searchParameters?.sortingOrder) {
                                SortingOrder.ASCENDING -> SortOrder.ASC
                                SortingOrder.DESCENDING -> SortOrder.DESC
                                null -> SortOrder.ASC
                            }
                )
                return@transaction sorted.limit(limit, offset).toList().map { User(it) } to User.selectAll().count()
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
                return@transaction User.selectAll()
                    .where { (User.admin eq true) or (User.mail eq config.superAdminMail) }
                    .map { User(it) }
            }
        }

        private fun sqlQualifyForDeletionProcess(): Op<Boolean> {
            return (User.admin eq false) and (User.username neq config.testUser.username)
        }

        fun oldLoginUsers(): List<User> {
            val cutoffDate = UnixTimestamp.now().addDays(-config.dataRetention.userDeactivateAfterDays)
            return transaction {
                return@transaction User.selectAll().where {
                    User.lastLogin less cutoffDate.toDB() and
                            (User.active eq true) and
                            sqlQualifyForDeletionProcess()

                }.map { User(it) }
            }
        }

        fun inactiveUsers(): List<User> {
            return transaction {
                return@transaction User.selectAll().where {
                    User.id inSubQuery UserDeletionQueue.select(UserDeletionQueue.userID).where {
                        UserDeletionQueue.currentDeletionStage eq UserDeletionQueue.DeletionStage.DEACTIVATION.value
                    }

                }.map { User(it) }
            }
        }

        fun usersToDelete(): List<User> {
            return transaction {
                return@transaction User.selectAll().where {
                    User.id inSubQuery UserDeletionQueue.select(UserDeletionQueue.userID).where {
                        (UserDeletionQueue.currentDeletionStage eq UserDeletionQueue.DeletionStage.DEACTIVATION.value) and (UserDeletionQueue.nextDeletionDate less UnixTimestamp.now()
                            .toDB())
                    } and (User.deleted.isNull())
                }.map { User(it) }
            }
        }

        fun usersToFinalDelete(): List<User> {
            return transaction {
                return@transaction User.selectAll().where {
                    User.id inSubQuery UserDeletionQueue.select(UserDeletionQueue.userID).where {
                        (UserDeletionQueue.currentDeletionStage eq UserDeletionQueue.DeletionStage.DELETION.value) and (UserDeletionQueue.nextDeletionDate less UnixTimestamp.now()
                            .toDB())
                    }
                }.map { User(it) }
            }
        }

        fun createUser(
            username: String?,
            firstName: String,
            lastName: String,
            mail: String,
            admin: Boolean = false
        ): User {
            val usernameToUse = getUniqueUsername(username)
            val user = User(usernameToUse, firstName, lastName, mail, true, admin)
            user.save()
            val poll = Poll.fromID(config.initialUserConfig.pollID)
            poll?.addUser(user.id)
            return user
        }

        fun getUniqueUsername(username: String?): String {
            var newUsername = username ?: generateRandomUsername()
            var i = 1
            while (byUsername(newUsername) != null) {
                newUsername = if (username == null) generateRandomUsername() else "$username$i"
                i++
            }
            return newUsername
        }

        fun empty(): User {
            return User(
                "empty",
                "empty",
                "empty",
                "empty",
                active = false,
                admin = false
            )
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
            firstName, lastName, username, id
        )
    }

    fun asUserInfo(): UserInfo {
        return UserInfo(
            id,
            username,
            firstName,
            lastName,
            mail,
            admin || superAdmin,
            superAdmin,
            active,
            oidConnections.mapIndexed { index: Int, it: OIDCUserData -> it.toConnectionOverview(index) }.toList(),
            created.toClient(),
            deleted?.toClient(),
            pollsOwned,
            maxPollsOwned,
            sessions.map { it.asSafeSession(null) }.toList(),
            lastLogin.toClient()
        )
    }

    fun asUserDataResponse(): UserDataResponse {
        return UserDataResponse(
            id,
            username,
            firstName,
            lastName,
            mail,
            active,
            admin,
            created.toClient(),
            pollsOwned,
            maxPollsOwned
        )
    }

    override fun equals(other: Any?): Boolean {
        if (other is User) return this.id == other.id
        if (other is SimpleUser) return this.id == other.id
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    @Deprecated("Use Poll.addUser instead", ReplaceWith("poll.addUser(userID)"))
    fun addPoll(pollID: tPollID) {
        val poll = Poll.fromID(pollID) ?: return
        poll.addUser(this.id)
    }

    fun removePoll(pollID: tPollID) {
        UserPolls.removeConnection(id, pollID)
    }

    fun deactivateUser() {
        if (!active) return
        active = false
        sessions.forEach { it.delete() }
        val deletionDate = UserDeletionQueue.deactivateUser(id)
        save()
        notifyInactivity(deletionDate)
    }

    fun reactivateUser() {
        if (deleted != null) return
        active = true
        UserDeletionQueue.removeUserFromDeletionQueue(id)
        save()
    }

    fun notifyInactivity(deletionDate: UnixTimestamp) {
        val mailData = ExpollMail.createUserDeactivationNotificationMail(this, deletionDate)
        Mail.sendMailAsync(mailData)
    }

    fun notifyDeletion() {
        val mailData = ExpollMail.createUserDeletionInformationMail(this)
        Mail.sendMailAsync(mailData)
    }
}

