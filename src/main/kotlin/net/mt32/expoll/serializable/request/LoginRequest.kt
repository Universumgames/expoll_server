package net.mt32.expoll.serializable.request

import kotlinx.serialization.Serializable

@Serializable
data class SimpleLoginRequest(
    val mail: String? = null,
    val otp: String? = null,
    val forApp: Boolean? = null,
    val version: String? = null,
    val platform: Platform = Platform.UNKNOWN
)

@Serializable
enum class Platform {
    ANDROID,
    IOS,
    WEB,
    UNKNOWN
}