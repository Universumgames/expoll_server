package net.mt32.expoll.serializable

import kotlinx.serialization.Serializable
import net.mt32.expoll.CompatibleVersionDescriptor
import net.mt32.expoll.config

@Serializable
data class ServerInfo(
    val version: String,
    val compatibleVersions: List<CompatibleVersionDescriptor>,
    val serverPort: Int,
    val frontendPort: Int,
    val loginLinkBase: String,
    val mailSender: String
) {
    companion object {
        val instance: ServerInfo
            get() = ServerInfo(
                config.serverVersion,
                config.compatibleVersions,
                config.serverPort,
                config.frontEndPort,
                config.loginLinkURL,
                config.mail.mailUser
            )
    }
}