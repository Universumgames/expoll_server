package net.mt32.expoll.serializable.admin.responses

import kotlinx.serialization.Serializable
import net.mt32.expoll.serializable.responses.IUserDataResponse
import net.mt32.expoll.serializable.responses.SafeSession
import net.mt32.expoll.tClientDateTime
import net.mt32.expoll.tUserID

@Serializable
data class UserInfo(
    override val id: tUserID,
    override val username: String,
    override val firstName: String,
    override val lastName: String,
    override val mail: String,
    override val admin: Boolean,
    val superAdmin: Boolean,
    override val active: Boolean,
    val oidcConnections: List<String>,
    override val createdTimestamp: tClientDateTime,
    val deletedTimestamp: tClientDateTime? = null,
    override val pollsOwned: Long,
    override val maxPollsOwned: Long,
    val sessions: List<SafeSession>,
    val lastLoginTimestamp: tClientDateTime
): IUserDataResponse

@Serializable
data class UserListResponse(
    val users: List<UserInfo>,
    val totalCount: Int
)