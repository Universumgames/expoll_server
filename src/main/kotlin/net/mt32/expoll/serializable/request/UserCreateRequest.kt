package net.mt32.expoll.serializable.request

import kotlinx.serialization.Serializable

@Serializable
data class UserCreateRequest (
    val firstName: String,
    val lastName: String,
    val mail: String,
    val username: String,
    val captcha: String?,
    val appAttest: String?,
)