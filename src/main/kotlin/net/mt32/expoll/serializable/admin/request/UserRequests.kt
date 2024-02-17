package net.mt32.expoll.serializable.admin.request

import kotlinx.serialization.Serializable
import net.mt32.expoll.tUserID

@Serializable
data class AdminCreateUserRequest(
    val mail: String,
    val firstName: String,
    val lastName: String,
    val username: String
)

@Serializable
data class AdminEditUserRequest(
    val userID: tUserID,
    val firstName: String? = null,
    val lastName: String? = null,
    val mail: String? = null,
    val username: String? = null,
    val admin: Boolean? = null,
    val maxPollsOwned: Long? = null,
    val active: Boolean? = null
)