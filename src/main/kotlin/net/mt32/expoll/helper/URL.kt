package net.mt32.expoll.helper

import io.ktor.server.application.*
import net.mt32.expoll.commons.tPollID
import net.mt32.expoll.config
import net.mt32.expoll.entities.OTP
import net.mt32.expoll.entities.User
import net.mt32.expoll.entities.UserDeletionConfirmation
import java.net.URLEncoder

object DeepLinkBuilder {
    fun buildLoginLink(call: ApplicationCall, otp: OTP, isNewUser: Boolean): String {
        return "expoll://login?key=" + URLEncoder.encode(otp.otp, "utf-8") + (if(isNewUser) "&isNewUser=1" else "")
    }
}

object URLBuilder {
    fun buildLoginLink(call: ApplicationCall, user: User, otp: OTP, isNewUser: Boolean): String {
        if(otp.forApp) return DeepLinkBuilder.buildLoginLink(call, otp, isNewUser)
        val protocol = call.request.local.scheme
        return protocol + "://" + config.loginLinkURL + "/#/login?key=" + URLEncoder.encode(otp.otp, "utf-8") + (if(isNewUser) "&isNewUser=1" else "")
    }

    fun shareURLBuilder(pollID: tPollID): String{
        val prefix = config.shareURLPrefix
        //if(!prefix.endsWith("/")) prefix += "/"
        return prefix + pollID
    }

    fun deleteConfirmationURL(call: ApplicationCall, confirmation: UserDeletionConfirmation): String{
        return config.deleteURLPrefix + confirmation.key
    }

    fun webSignupURL(call: ApplicationCall, mail: String): String{
        return "http://" + config.loginLinkURL + "/#/login?mail=" + mail
    }
}

