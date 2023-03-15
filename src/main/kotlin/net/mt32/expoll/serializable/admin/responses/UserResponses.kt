package net.mt32.expoll.serializable.admin.responses

import kotlinx.serialization.Serializable
import net.mt32.expoll.tUserID

@Serializable
data class UserInfo(
    val id: tUserID,
    val username: String,
    val firstName: String,
    val lastName: String,
    val mail: String,
    val admin: Boolean,
    val superAdmin: Boolean,
    val active: Boolean
)

@Serializable
data class UserListResponse(
    val users: List<UserInfo>,
    val totalCount: Int
)