package net.mt32.expoll.entities

import net.mt32.expoll.tUserID


data class Challenge(
    val id: String,
    val challenge: String,
    val userID: tUserID
)


data class Webauthn(
    val userID: tUserID,
    val credentialID: String,
    val credentialPublicKey: String,
    var counter: Int,
    val transports: String,
    var name: String,
    val initiatorPlatform: String,
    val createdTimestamp: Long
)