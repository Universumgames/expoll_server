package net.mt32.expoll.serializable.admin.request

import kotlinx.serialization.Serializable


@Serializable
data class SimpleMailRegexRule(
    val regex: String,
    val blacklist: Boolean
)

@Serializable
data class MailRegexEditRequest(
    val mailRegex: List<SimpleMailRegexRule>
)