package net.mt32.expoll.helper

import io.ktor.server.application.*
import net.mt32.expoll.config
import java.net.URLEncoder

/**
 * Build login url sent vie mail
 * @param {Request} req express request object
 * @param {string} loginKey the users login key
 * @return {string} the login url
 */
fun urlBuilder(call: ApplicationCall, loginKey: String): String {
    val port = config.frontEndPort
    val protocol = call.request.local.scheme
    return protocol +
            "://" +
            config.loginLinkURL +
            (if (port == 80 || port == 443) "" else ":$port") + "/#/login?key=" +
            URLEncoder.encode(loginKey, "utf-8")
}