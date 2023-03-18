package net.mt32.expoll

import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.util.*

object Mail {
    fun sendMail(to: String, subject: String, body: String) {
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
            message.setFrom(InternetAddress(config.mailUser))
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
            message.subject = subject
            message.setContent(body, "text/plain")


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