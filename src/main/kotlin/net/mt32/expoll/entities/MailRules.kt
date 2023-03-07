package net.mt32.expoll.entities

import net.mt32.expoll.database.DatabaseEntity
import net.mt32.expoll.database.UUIDLength
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class MailRule : DatabaseEntity {
    val id: String
    val regex: String
    val blacklist: Boolean

    constructor(id: String, regex: String, blacklist: Boolean) : super() {
        this.id = id
        this.regex = regex
        this.blacklist = blacklist
    }

    private constructor(ruleRow: ResultRow) {
        this.id = ruleRow[MailRule.id]
        this.regex = ruleRow[MailRule.regex]
        this.blacklist = ruleRow[MailRule.blacklist]
    }

    override fun save() {
        TODO("Not yet implemented")
    }

    companion object : Table("mail_regex_rules") {
        val id = varchar("id", UUIDLength)
        val regex = varchar("regex", 255)
        val blacklist = bool("blacklist")

        override val primaryKey = PrimaryKey(id)

        /**
         * get all mail regex rules
         */
        fun all(): List<MailRule> {
            return transaction {
                val result = MailRule.selectAll()
                return@transaction result.map { MailRule(it) }
            }
        }

        /**
         * Check if a mail is not banned
         * @param mail the mial adress to check
         * @param regexRules not allowed mail adresss
         * @return true if mail is allowed, false otherwise
         */
        fun mailIsAllowed(mail: String): Boolean {
            var res = true
            for (regex in all()) {
                if ((mail.matches(regex.regex.toRegex()) && regex.blacklist) ||
                    (!mail.matches(regex.regex.toRegex()) && !regex.blacklist)
                ) {
                    res = false
                }
            }
            return res
        }
    }
}