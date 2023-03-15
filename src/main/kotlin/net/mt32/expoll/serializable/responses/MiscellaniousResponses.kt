package net.mt32.expoll.serializable.responses

import kotlinx.serialization.Serializable
import net.mt32.expoll.entities.MailRule

@Serializable
data class MailRegexRules(
    val regex: List<MailRule>
)
