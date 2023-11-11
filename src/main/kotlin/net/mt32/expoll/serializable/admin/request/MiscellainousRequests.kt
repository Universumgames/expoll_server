package net.mt32.expoll.serializable.admin.request

import kotlinx.serialization.Serializable
import net.mt32.expoll.entities.PollSearchParameters
import net.mt32.expoll.entities.UserSearchParameters


@Serializable
data class SimpleMailRegexRule(
    val regex: String,
    val blacklist: Boolean
)

@Serializable
data class MailRegexEditRequest(
    val mailRegex: List<SimpleMailRegexRule>
)

interface AdminListRequest {
    val limit: Int
    val offset: Long
}

@Serializable
data class AdminUserListRequest(
    override val limit: Int = 100,
    override val offset: Long = 0,
    val searchParameters: UserSearchParameters = UserSearchParameters()
): AdminListRequest

@Serializable
data class AdminPollListRequest(
    override val limit: Int = 100,
    override val offset: Long = 0,
    val searchParameters: PollSearchParameters = PollSearchParameters()
): AdminListRequest
