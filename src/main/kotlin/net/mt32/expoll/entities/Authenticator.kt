package net.mt32.expoll.entities

import net.mt32.expoll.database.DatabaseEntity
import net.mt32.expoll.database.UUIDLength
import net.mt32.expoll.helper.UnixTimestamp
import net.mt32.expoll.helper.toUnixTimestamp
import net.mt32.expoll.helper.upsert
import net.mt32.expoll.tUserID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction


class Challenge : DatabaseEntity {
    val id: Int
    val challenge: String
    val userID: tUserID

    constructor(id: Int, challenge: String, userID: tUserID) {
        this.id = id
        this.challenge = challenge
        this.userID = userID
    }

    constructor(challenge: String, userID: tUserID) {
        this.id = newUnusedID()
        this.challenge = challenge
        this.userID = userID
    }

    private constructor(challengeRow: ResultRow) {
        this.id = challengeRow[Challenge.id]
        this.challenge = challengeRow[Challenge.challenge]
        this.userID = challengeRow[Challenge.userID]
    }

    override fun save(): Boolean {
        Challenge.upsert(Challenge.id) {
            it[id] = this@Challenge.id
            it[challenge] = this@Challenge.challenge
            it[userID] = this@Challenge.userID
        }
        return true
    }

    override fun delete(): Boolean {
        transaction {
            Challenge.deleteWhere{
                Challenge.id eq this@Challenge.id
            }
        }
        return true
    }

    companion object : Table("challenge") {
        val id = integer("id").autoIncrement()
        val challenge = varchar("challenge", 255)
        val userID = varchar("userId", UUIDLength)

        override val primaryKey = PrimaryKey(id)

        fun fromID(id: Int): Challenge? {
            return transaction {
                val challengeRow = Challenge.select { Challenge.id eq id }.firstOrNull()
                return@transaction challengeRow?.let { Challenge(it) }
            }
        }

        fun forUser(userID: String): List<Challenge> {
            return transaction {
                val challengeRow = Challenge.select { Challenge.userID eq userID }
                return@transaction challengeRow.map { Challenge(it) }
            }
        }

        private fun newUnusedID(): Int {
            return transaction {
                var id = 0
                do {
                    id++
                } while (fromID(id) != null)
                return@transaction id
            }
        }
    }
}


class Authenticator : DatabaseEntity {
    val userID: tUserID
    val credentialID: String
    val credentialPublicKey: String
    var counter: Int
    val transports: List<String>
    var name: String
    val initiatorPlatform: String
    val createdTimestamp: UnixTimestamp

    constructor(
        userID: tUserID,
        credentialID: String,
        credentialPublicKey: String,
        counter: Int,
        transports: List<String>,
        name: String,
        initiatorPlatform: String,
        createdTimestamp: UnixTimestamp
    ) {
        this.userID = userID
        this.credentialID = credentialID
        this.credentialPublicKey = credentialPublicKey
        this.counter = counter
        this.transports = transports
        this.name = name
        this.initiatorPlatform = initiatorPlatform
        this.createdTimestamp = createdTimestamp
    }

    constructor(authRow: ResultRow) {
        this.userID = authRow[Authenticator.userID]
        this.credentialID = authRow[Authenticator.credentialID]
        this.credentialPublicKey = authRow[Authenticator.credentialPublicKey]
        this.counter = authRow[Authenticator.counter]
        this.transports = authRow[Authenticator.transports].split(",")
        this.name = authRow[Authenticator.name]
        this.initiatorPlatform = authRow[Authenticator.initiatorPlatform]
        this.createdTimestamp = authRow[Authenticator.createdTimestamp].toUnixTimestamp()
    }

    override fun save(): Boolean {
        transaction {
            Authenticator.upsert(Authenticator.credentialID) {
                it[Authenticator.userID] = this@Authenticator.userID
                it[Authenticator.credentialID] = this@Authenticator.credentialID
                it[Authenticator.credentialPublicKey] = this@Authenticator.credentialPublicKey
                it[Authenticator.counter] = this@Authenticator.counter
                it[Authenticator.transports] = this@Authenticator.transports.joinToString(",")
                it[Authenticator.name] = this@Authenticator.name
                it[Authenticator.initiatorPlatform] = this@Authenticator.initiatorPlatform
                it[Authenticator.createdTimestamp] = this@Authenticator.createdTimestamp.toDB()
            }
        }
        return true
    }

    override fun delete(): Boolean {
        transaction {
            Authenticator.deleteWhere {
                Authenticator.credentialID eq this@Authenticator.credentialID
            }
        }
        return true
    }

    companion object : Table("authenticator") {
        val userID = varchar("userId", UUIDLength)
        val credentialID = varchar("credentialID", 255)
        val name = varchar("name", 255)
        val credentialPublicKey = varchar("credentialPublicKey", 255)
        val createdTimestamp = long("createdTimestamp")
        val counter = integer("counter")
        val transports = varchar("transports", 255)
        val initiatorPlatform = varchar("initiatorPlatform", 512)

        override val primaryKey = PrimaryKey(credentialID)

        fun fromUser(userID: String): List<Authenticator> {
            return transaction {
                val result = Authenticator.select { Authenticator.userID eq userID }
                return@transaction result.map { Authenticator(it) }
            }
        }

        fun fromCredentialID(credentialID: String): Authenticator? {
            return transaction {
                val result = Authenticator.select { Authenticator.credentialID eq credentialID }.firstOrNull()
                return@transaction result?.let { Authenticator(it) }
            }
        }
    }
}