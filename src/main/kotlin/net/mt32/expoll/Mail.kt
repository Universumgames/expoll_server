package net.mt32.expoll

import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import net.mt32.expoll.entities.UserDeletionConfirmation
import net.mt32.expoll.helper.UnixTimestamp
import java.util.*

object ExpollMail {
    fun OTPMail(mail: String, fullName: String, otp: String, loginLink: String): Mail.MailData {
        return Mail.MailData(
            mail, fullName, "Login to expoll", "Here is your OTP for logging in on the expoll website: \n\t" +
                    otp +
                    "\n alternatively you can click this link \n" + loginLink
        )
    }

    fun UserCreationMail(mail: String, fullName: String, scheme: String): Mail.MailData {
        val port = config.frontEndPort
        return Mail.MailData(
            mail, fullName, "Thank you for registering in expoll",
            "Thank you for creating an account at over at expoll (" +
                    scheme +
                    "://" +
                    config.loginLinkURL +
                    (if (port == 80 || port == 443) "" else ":$port") +
                    ")"
        )
    }

    fun UserDeactivationNotificationMail(mail: String, fullName: String): Mail.MailData {
        return Mail.MailData(
            mail, fullName, "Your account has been deactivated",
            "Your account has been deactivated because you haven't used the service in a long time. To avoid account deletion, please log in.\n" +
                    "If you don't login within the next " + config.dataRetention.userDeleteAfterAdditionalDays + " days (by ${UnixTimestamp.now().addDays(
                config.dataRetention.userDeleteAfterAdditionalDays).toDate().toString()}), your account will be deleted.")
    }

    fun UserDeletionInformationMail(mail: String, fullName: String, confirmation: UserDeletionConfirmation): Mail.MailData {
        return Mail.MailData(
            mail, fullName, "Your account has been deleted",
            "Your account has been deleted. All your personal information has been anonymized and is no longer associated with you. If you didn't have any polls, your account has been deleted immediately. If you had polls, your account has been deleted completely"
        )
    }
}

object Mail {

    data class MailData(val toMail: String, val toName: String, val subject: String, val body: String)

    fun sendMailAsync(toMail: String, toName: String, subject: String, body: String) {
        //mailQueue.add(MailData(to, subject, body))
        Thread {
            sendMail(MailData(toMail, toName, subject, body))
        }.start()
    }

    fun sendMailAsync(mail: MailData) {
        //mailQueue.add(MailData(to, subject, body))
        Thread {
            sendMail(mail)
        }.start()
    }

    private fun sendMail(data: MailData) {
        val config = config.mail

        try {
            val props = Properties()
            props["mail.transport.protocol"] = "smtp"
            props["mail.smtp.host"] = config.mailServer
            props["mail.smtp.port"] = config.mailPort
            props["mail.smtp.auth"] = "true"
            props["mail.smtp.starttls.enable"] = config.mailPort
            props["mail.smtp.starttls.enable"] = "true"
            props.setProperty("mail.debug", "false");
            props.setProperty("mail.smtp.ssl.protocols", "TLSv1.2")


            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(config.mailUser, config.mailPassword)
                }
            })

            val message = MimeMessage(session)
            message.setFrom(InternetAddress(config.mailUser, "Expoll"))
            message.setRecipients(Message.RecipientType.TO, arrayOf(InternetAddress(data.toMail, data.toName)))
            message.subject = data.subject
            message.setContent(data.body, "text/plain")


            //Transport.send(message)
            val transport: Transport = session.getTransport("smtp")

            transport.connect(config.mailServer, config.mailPort, config.mailUser, config.mailPassword)
            transport.sendMessage(message, message.allRecipients)
            transport.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}