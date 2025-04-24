package net.mt32.expoll.entities

import net.mt32.expoll.database.DatabaseEntity
import net.mt32.expoll.database.UUIDLength
import net.mt32.expoll.commons.helper.UnixTimestamp
import net.mt32.expoll.helper.upsertCustom
import net.mt32.expoll.commons.tUserID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
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
            DeleteConfirmation.upsertCustom(DeleteConfirmation.id) {
                it[DeleteConfirmation.id] = this@DeleteConfirmation.id
                it[DeleteConfirmation.userID] = this@DeleteConfirmation.userID
                it[DeleteConfirmation.expirationTimestamp] = this@DeleteConfirmation.expirationTimestamp.toDB()
            }
        }
        return true
    }

    override fun delete(): Boolean {
        transaction {
            DeleteConfirmation.deleteWhere { DeleteConfirmation.id eq this@DeleteConfirmation.id }
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