package net.mt32.expoll

import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.util.*

object Mail {

    internal data class MailData(val to: String, val subject: String, val body: String)

    //private val mailThread: Thread
    //private val mailQueue: MutableList<MailData>

    init {
        //mailQueue = mutableListOf()
        /*mailThread = Thread {
            while (true) {
                val mail = mailQueue.removeFirstOrNull()
                if (mail != null) sendMail(mail)
            }
        }
        mailThread.start()*/
    }

    fun sendMail(to: String, subject: String, body: String) {
        //mailQueue.add(MailData(to, subject, body))
        Thread{
            sendMail(MailData(to, subject, body))
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
            message.setFrom(InternetAddress(config.mailUser))
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(data.to))
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