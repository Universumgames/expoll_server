package net.mt32.expoll.entities

import net.mt32.expoll.config
import net.mt32.expoll.database.DatabaseEntity
import net.mt32.expoll.database.UUIDLength
import net.mt32.expoll.helper.UnixTimestamp
import net.mt32.expoll.helper.toUnixTimestampFromDB
import net.mt32.expoll.helper.upsert
import net.mt32.expoll.serializable.responses.SimpleUser
import net.mt32.expoll.tPollID
import net.mt32.expoll.tUserID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

interface IUser {
    val id: tUserID
    var username: String
    var firstName: String
    var lastName: String
    var mail: String
    var polls: List<Poll>
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
    override var mail: String
    override var polls: List<Poll>
        get() {
            return cachedPolls ?: Poll.accessibleForUser(id)
        }
        set(value) {
            cachedPolls = value
        }
    override val votes: List<Vote>
        get() = Vote.fromUser(this)

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

    val created: UnixTimestamp?

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
    }

    private constructor(userRow: ResultRow) {
        this.id = userRow[User.id]
        this.username = userRow[User.username]
        this.mail = userRow[User.mail]
        this.firstName = userRow[User.firstName]
        this.lastName = userRow[User.lastName]
        this.active = userRow[User.active]
        this.admin = userRow[User.admin] || config.superAdminMail.equals(mail, ignoreCase = true)
        this.created = userRow[User.created]?.toUnixTimestampFromDB()
    }

    override fun save(): Boolean {
        transaction {
            User.upsert(User.id) {
                it[id] = this@User.id
                it[username] = this@User.username
                it[mail] = this@User.mail
                it[firstName] = this@User.firstName
                it[lastName] = this@User.lastName
                it[active] = this@User.active
                it[admin] = this@User.admin
                it[created] = this@User.created?.toDB()
            }

            val polls = this@User.polls
            UserPolls.deleteWhere { userID eq id }
            UserPolls.batchInsert(polls.map { poll -> poll.id }) { pollID ->
                this[UserPolls.pollID] = pollID
                this[UserPolls.userID] = id
            }

            sessions.forEach { it.save() }
            challenges.forEach { it.save() }
            authenticators.forEach { it.save() }
            votes.forEach { it.save() }
        }
        return true
    }

    override fun delete(): Boolean {
        TODO("Not yet implemented")
    }

    /**
     * Creates a new session and saves it
     */
    fun createSession(): Session {
        val session = Session.createSession(id)
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
        val created = long("createdTimestamp").nullable()


        override val primaryKey = PrimaryKey(id)

        fun loadFromID(id: String): User? {
            return transaction {
                val userRow = User.select { User.id eq id }.limit(1).firstOrNull()
                return@transaction userRow?.let { User(it) }
            }
        }

        fun loadFromLoginKey(loginKey: String): User? {
            return transaction {
                val sessionRow =
                    Session.select { Session.loginKey eq loginKey }.firstOrNull() ?: return@transaction null
                val userRow = User.select { User.id eq sessionRow[Session.userID] }.firstOrNull()
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
    }

    fun asSimpleUser(): SimpleUser {
        return SimpleUser(
            firstName,
            lastName,
            username,
            id
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
}