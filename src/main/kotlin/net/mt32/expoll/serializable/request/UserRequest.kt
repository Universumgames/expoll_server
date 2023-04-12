package net.mt32.expoll.serializable.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateUserRequest (
    val firstName: String,
    val lastName: String,
    val mail: String,
    val username: String,
    val captcha: String? = null,
    val appAttest: String? = null,
)

@Serializable
data class EditUserRequest(
    val username: String? = null,
    val firstName: String? = null,
    val lastName: String? = null
)