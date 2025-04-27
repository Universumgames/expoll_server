package net.mt32.expoll

import net.mt32.expoll.commons.serializable.ServerInfo

val ServerInfo.Companion.instance: ServerInfo
    get() = ServerInfo(
        config.serverVersion,
        config.compatibleVersions,
        config.serverPort,
        config.frontEndPort,
        config.loginLinkURL,
        config.mail.mailUser
    )

