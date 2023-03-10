package net.mt32.expoll.entities

import net.mt32.expoll.database.DatabaseEntity
import net.mt32.expoll.database.UUIDLength
import net.mt32.expoll.helper.UnixTimestamp
import net.mt32.expoll.tUserID
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction

class DeleteConfirmation : DatabaseEntity {
    val id: String
    val userID: tUserID
    val expirationTimestamp: UnixTimestamp

    constructor(id: String, userID: tUserID, expirationTimestamp: UnixTimestamp) {
        this.id = id
        this.userID = userID
        this.expirationTimestamp = expirationTimestamp
    }

    override fun save(): Boolean {
        transaction {
            TODO("Not yet implemented")
        }
        return true
    }

    companion object : Table("delete_confirmation") {
        val id = varchar("id", UUIDLength)
        val userID = varchar("userId", UUIDLength)
        val expirationTimestamp = long("expirationTimestamp")

        override val primaryKey = PrimaryKey(id)
    }
}