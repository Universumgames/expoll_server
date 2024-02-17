package net.mt32.expoll.entities

import kotlinx.serialization.Serializable
import net.mt32.expoll.database.DatabaseEntity
import net.mt32.expoll.database.UUIDLength
import net.mt32.expoll.helper.upsertCustom
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class OIDCUserData : DatabaseEntity {
    val userID: String
    val idpName: String
    val issuer: String
    val subject: String
    val mail: String?

    constructor(userID: String, idpName: String, issuer: String, subject: String, mail: String?) : super() {
        this.userID = userID
        this.idpName = idpName
        this.issuer = issuer
        this.subject = subject
        this.mail = mail
    }

    private constructor(row: ResultRow) {
        this.userID = row[OIDCUserData.userID]
        this.idpName = row[OIDCUserData.idpName]
        this.issuer = row[OIDCUserData.issuer]
        this.subject = row[OIDCUserData.subject]
        this.mail = row[OIDCUserData.mail]
    }


    override fun save(): Boolean {
        transaction {
            OIDCUserData.upsertCustom(OIDCUserData.subject, OIDCUserData.idpName) {
                it[OIDCUserData.userID] = this@OIDCUserData.userID
                it[OIDCUserData.idpName] = this@OIDCUserData.idpName
                it[OIDCUserData.issuer] = this@OIDCUserData.issuer
                it[OIDCUserData.subject] = this@OIDCUserData.subject
                it[OIDCUserData.mail] = this@OIDCUserData.mail
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

    fun toConnectionOverview(): OIDCConnection {
        return OIDCConnection(idpName, mail, subject)
    }

    companion object : Table("oidcUserData") {
        val userID = varchar("userid", UUIDLength)
        val idpName = varchar("idp", 64)
        val issuer = varchar("issuer", 256)
        val subject = varchar("subject", 512)
        val mail = varchar("mail", 256).nullable()

        fun bySubjectAndIDP(subject: String, idpName: String): OIDCUserData? {
            return transaction {
                val row =
                    OIDCUserData.selectAll().where { (OIDCUserData.subject eq subject) and (OIDCUserData.idpName eq idpName) }
                        .firstOrNull()
                return@transaction row?.let { OIDCUserData(it) }
            }
        }

        fun byUser(userID: String): List<OIDCUserData> {
            return transaction {
                val result = OIDCUserData.selectAll().where { (OIDCUserData.userID eq userID) }
                return@transaction result.map { OIDCUserData(it) }
            }
        }
    }
}

@Serializable
data class OIDCConnection(
    val name: String,
    val mail: String?,
    val subject: String
)