package net.mt32.expoll.entities

import kotlinx.serialization.Serializable
import net.mt32.expoll.config

@Serializable
data class ServerInfo(
    val version: String,
    val minimumRequiredVersion: String,
    val serverPort: Int,
    val frontendPort: Int,
    val loginLinkBase: String,
    val mailSender: String
) {
    companion object {
        val instance: ServerInfo
            get() = ServerInfo(
                config.serverVersion,
                config.minimumRequiredClientVersion,
                config.serverPort,
                config.frontEndPort,
                config.loginLinkURL,
                config.mail.mailUser
            )
    }
}