package net.mt32.expoll.serializable.responses

import kotlinx.serialization.Serializable

@Serializable
data class UserDataResponse(
    val id: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    val mail: String,
    val active: Boolean,
    val admin: Boolean,
)