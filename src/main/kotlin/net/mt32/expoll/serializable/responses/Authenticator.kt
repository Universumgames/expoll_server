package net.mt32.expoll.serializable.responses

import kotlinx.serialization.Serializable
import net.mt32.expoll.tClientDateTime

@Serializable
data class SimpleAuthenticator(
    val credentialID: String,
    val name: String,
    val initiatorPlatform: String,
    val created: tClientDateTime
)

@Serializable
data class SimpleAuthenticatorList(
    val authenticators: List<SimpleAuthenticator>
)