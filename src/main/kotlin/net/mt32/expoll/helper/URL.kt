package net.mt32.expoll.helper

import io.ktor.server.application.*
import net.mt32.expoll.config
import net.mt32.expoll.tPollID
import java.net.URLEncoder

/**
 * Build login url sent vie mail
 * @param {Request} req express request object
 * @param {string} loginKey the users login key
 * @return {string} the login url
 */
fun urlBuilder(call: ApplicationCall, otp: String, forApp: Boolean = false): String {
    if(forApp){
        return "expoll://login?key=" + URLEncoder.encode(otp, "utf-8")
    }
    val port = config.frontEndPort
    val protocol = call.request.local.scheme
    return protocol +
            "://" +
            config.loginLinkURL +
           "/#/login?key=" +
            URLEncoder.encode(otp, "utf-8")
}

fun shareURLBuilder(pollID: tPollID): String{
    val prefix = config.shareURLPrefix
    //if(!prefix.endsWith("/")) prefix += "/"
    return prefix + pollID
}