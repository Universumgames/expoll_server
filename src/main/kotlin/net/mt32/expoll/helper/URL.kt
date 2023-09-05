package net.mt32.expoll.helper

import io.ktor.server.application.*
import net.mt32.expoll.config
import net.mt32.expoll.tPollID
import java.net.URLEncoder

object DeepLinkBuilder {
    fun buildLoginLink(call: ApplicationCall, otp: String): String {
        return "expoll://login?key=" + URLEncoder.encode(otp, "utf-8")
    }
}

object URLBuilder {
    fun buildLoginLink(call: ApplicationCall, otp: String): String {
        val protocol = call.request.local.scheme
        return protocol + "://" + config.loginLinkURL + "/#/login?key=" + URLEncoder.encode(otp, "utf-8")
    }

    fun shareURLBuilder(pollID: tPollID): String{
        val prefix = config.shareURLPrefix
        //if(!prefix.endsWith("/")) prefix += "/"
        return prefix + pollID
    }
}

