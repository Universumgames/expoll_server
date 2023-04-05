package net.mt32.expoll.entities

import net.mt32.expoll.database.DatabaseEntity
import net.mt32.expoll.database.UUIDLength
import net.mt32.expoll.helper.upsert
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class OIDCUserData : DatabaseEntity {
    val userID: String
    val idpName: String
    val issuer: String
    val subject: String

    constructor(userID: String, idpName: String, issuer: String, subject: String) : super() {
        this.userID = userID
        this.idpName = idpName
        this.issuer = issuer
        this.subject = subject
    }

    private constructor(row: ResultRow) {
        this.userID = row[OIDCUserData.userID]
        this.idpName = row[OIDCUserData.idpName]
        this.issuer = row[OIDCUserData.issuer]
        this.subject = row[OIDCUserData.subject]
    }


    override fun save(): Boolean {
        transaction {
            OIDCUserData.upsert(OIDCUserData.subject, OIDCUserData.idpName) {
                it[OIDCUserData.userID] = this@OIDCUserData.userID
                it[OIDCUserData.idpName] = this@OIDCUserData.idpName
                it[OIDCUserData.issuer] = this@OIDCUserData.issuer
                it[OIDCUserData.subject] = this@OIDCUserData.subject
            }
        }
        return true
    }

    override fun delete(): Boolean {
        transaction {
            deleteWhere {
                (OIDCUserData.subject eq this@OIDCUserData.subject) and
                        (OIDCUserData.idpName eq this@OIDCUserData.idpName) and
                        (OIDCUserData.userID eq this@OIDCUserData.userID)
            }
        }
        return true
    }

    companion object : Table("oidcUserData") {
        val userID = varchar("userid", UUIDLength)
        val idpName = varchar("idp", 64)
        val issuer = varchar("issuer", 256)
        val subject = varchar("subject", 512)

        fun bySubjectAndIDP(subject: String, idpName: String): OIDCUserData? {
            return transaction {
                val row =
                    OIDCUserData.select { (OIDCUserData.subject eq subject) and (OIDCUserData.idpName eq idpName) }
                        .firstOrNull()
                return@transaction row?.let { OIDCUserData(it) }
            }
        }
    }
}