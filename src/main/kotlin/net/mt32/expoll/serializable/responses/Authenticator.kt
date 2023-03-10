package net.mt32.expoll.serializable.responses

import kotlinx.serialization.Serializable

@Serializable
data class SimpleAuthenticator(
    val credentialID: String,
    val name: String,
    val initiatorPlatform: String,
    val created: String
)

@Serializable
data class SimpleAuthenticatorList(
    val authenticators: List<SimpleAuthenticator>
)