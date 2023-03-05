package net.mt32.expoll.entities

import net.mt32.expoll.DatabaseEntity
import net.mt32.expoll.UUIDLength
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.Authenticator
import java.util.*

interface IUser {
    val id: String
    var username: String
    var firstName: String
    var lastName: String
    var mail: String
    var polls: List<Poll>
    val votes: List<Vote>
    var session: List<Session>
    var notes: List<PollUserNote>
    var active: Boolean
    var admin: Boolean
    var challenges: List<Challenge>
    var authenticators: List<Authenticator>
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
            return cachedPolls ?: mutableListOf()
        }
        set(value) {
            cachedPolls = value
        }
    override val votes: List<Vote>
        get() = Vote.fromUser(this)

    override var session: List<Session> = mutableListOf()
    override var notes: List<PollUserNote> = mutableListOf()
    override var active: Boolean
    override var admin: Boolean
    override var challenges: List<Challenge> = mutableListOf()
    override var authenticators: List<Authenticator> = mutableListOf()

    private var cachedPolls: List<Poll>? = null

    private constructor(userRow: ResultRow) {
        this.id = userRow[Users.id]
        this.username = userRow[Users.username]
        this.mail = userRow[Users.mail]
        this.firstName = userRow[Users.firstName]
        this.lastName = userRow[Users.lastName]
        this.active = userRow[Users.active]
        this.admin = userRow[Users.admin]
    }

    constructor(
        username: String,
        firstName: String,
        lastName: String,
        mail: String,
        active: Boolean = true,
        admin: Boolean,

        ) {
        this.id = UUID.randomUUID().toString()
        this.username = username
        this.firstName = firstName
        this.lastName = lastName
        this.mail = mail
        this.active = active
        this.admin = admin

    }

    companion object {
        fun loadFromID(id: String): User? {
            return transaction {
                val userRow = Users.select { Users.id eq id }.limit(1).firstOrNull()
                if (userRow != null) return@transaction User(userRow) else null
            }
        }

        fun loadFromLoginKey(loginKey: String): User? {
            return transaction {
                val sessionRow =
                    Sessions.select { Sessions.loginKey eq loginKey }.firstOrNull() ?: return@transaction null
                val userRow = Users.select { Users.id eq sessionRow[Sessions.userID] }.firstOrNull()
                if (userRow != null) return@transaction User(userRow) else null
            }
        }
    }

    override fun save() {
        TODO("Not yet implemented")
    }

    override fun saveRecursive() {

    }
}


object Users : Table("user") {
    const val maxUserNameLength = 255
    const val maxNameComponentLength = 255
    const val maxMailLength = 255


    val id = varchar("id", UUIDLength).default(UUID.randomUUID().toString())
    val username = varchar("username", maxUserNameLength)
    val firstName = varchar("firstName", maxNameComponentLength)
    val lastName = varchar("lastName", maxNameComponentLength)
    val mail = varchar("mail", maxMailLength)
    val active = bool("active")
    val admin = bool("admin")


    override val primaryKey = PrimaryKey(id)

}