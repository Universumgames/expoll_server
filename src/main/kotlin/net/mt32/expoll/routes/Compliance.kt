package net.mt32.expoll.routes

import kotlinx.serialization.Serializable

@Serializable
data class Compliance(
    val version: String,
    val build: String? = null,
    val platform: String? = null,
)
