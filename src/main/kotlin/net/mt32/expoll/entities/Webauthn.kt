package net.mt32.expoll.entities

import net.mt32.expoll.database.DatabaseEntity
import net.mt32.expoll.database.UUIDLength
import net.mt32.expoll.tUserID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction


class Challenge: DatabaseEntity{
    val id: String
    val challenge: String
    val userID: tUserID

    constructor(id: String, challenge: String, userID: tUserID) {
        this.id = id
        this.challenge = challenge
        this.userID = userID
    }

    constructor(challengeRow: ResultRow){
        this.id = challengeRow[Challenge.id]
        this.challenge = challengeRow[Challenge.challenge]
        this.userID = challengeRow[Challenge.userID]
    }

    override fun save(){
        TODO("Not implemented yet")
    }

    companion object: Table("challenge"){
        val id = varchar("id", 255)
        val challenge = varchar("challenge", 255)
        val userID = varchar("userId", UUIDLength)

        override val primaryKey = PrimaryKey(id)

        fun fromID(id: String): Challenge? {
            return transaction{
                val challengeRow = Challenge.select{ Challenge.id eq id}.firstOrNull()
                if(challengeRow != null) return@transaction Challenge(challengeRow) else return@transaction null
            }
        }
    }
}


class Webauthn: DatabaseEntity{
    val userID: tUserID
    val credentialID: String
    val credentialPublicKey: String
    var counter: Int
    val transports: String
    var name: String
    val initiatorPlatform: String
    val createdTimestamp: Long

    constructor(
        userID: tUserID,
        credentialID: String,
        credentialPublicKey: String,
        counter: Int,
        transports: String,
        name: String,
        initiatorPlatform: String,
        createdTimestamp: Long
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

    // TODO implement authenticator table
    /*constructor(authRow: ResultRow){
        this.userID = userID
        this.credentialID = credentialID
        this.credentialPublicKey = credentialPublicKey
        this.counter = counter
        this.transports = transports
        this.name = name
        this.initiatorPlatform = initiatorPlatform
        this.createdTimestamp = createdTimestamp
    }*/

    override fun save() {
        TODO("Not yet implemented")
    }

    companion object: Table("authenticator"){

        // TODO implement
        fun fromUser(userID: String): Array<Webauthn>{
            return arrayOf()
        }
    }
}