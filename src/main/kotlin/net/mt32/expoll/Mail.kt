package net.mt32.expoll

import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import kotlinx.serialization.encodeToString
import net.mt32.expoll.entities.OTP
import net.mt32.expoll.entities.User
import net.mt32.expoll.helper.UnixTimestamp
import net.mt32.expoll.helper.defaultJSON
import net.mt32.expoll.serializable.responses.UserPersonalizeResponse
import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect
import org.thymeleaf.TemplateEngine
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import org.thymeleaf.templateresolver.ITemplateResolver
import java.util.*

object ExpollMail {
    fun OTPMail(user: User, otp: OTP, loginLink: String): Mail.MailData {
        return Mail.MailData(
            user.mail, user.fullName, "Login to expoll",
            Mail.fromTemplate(
                Mail.Template.OTP,
                mapOf(
                    "otp" to otp.otp,
                    "loginLink" to loginLink,
                    "title" to "Login",
                    "forApp" to otp.forApp
                )
            ),
            true
        )
    }

    fun UserCreationMail(user: User, scheme: String): Mail.MailData {
        val port = config.frontEndPort
        val data = mapOf(
            "scheme" to scheme,
            "port" to port,
            "user" to user,
            "title" to "Welcome"
        )
        val body =
            Mail.fromTemplate(Mail.Template.USER_CREATION, data)
        return Mail.MailData(
            user.mail, user.fullName,
            "Thank you for registering in expoll", body,
            true
        )
    }

    fun UserDeactivationNotificationMail(user: User, deletionDate: UnixTimestamp): Mail.MailData {
        val data = mapOf(
            "user" to user,
            "title" to "Account Deactivation",
            "userDeleteAfterAdditionalDays" to config.dataRetention.userDeleteAfterAdditionalDays,
            "userDeleteAfterDate" to deletionDate.toDate().toString()
        )
        return Mail.MailData(
            user.mail, user.fullName,
            "Your account has been deactivated",
            Mail.fromTemplate(Mail.Template.USER_DEACTIVATION, data),
            true
        )
    }

    fun UserDeletionInformationMail(
        user: User
    ): Mail.MailData {
        return Mail.MailData(
            user.mail, user.fullName, "Your account has been deleted",
            Mail.fromTemplate(
                Mail.Template.USER_DELETION,
                mapOf("user" to user, "title" to "Account Deletion")
            ),
            true
        )
    }

    fun PersonalDataMail(user: User, personalizeResponse: UserPersonalizeResponse): Mail.MailData {
        val data = mapOf(
            "user" to user,
            "personalizeResponse" to personalizeResponse,
            "personalizeResponseJSON" to defaultJSON.encodeToString(personalizeResponse),
            "title" to "Personal Data"
        )
        return Mail.MailData(
            user.mail, user.fullName,
            "Your personal data",
            Mail.fromTemplate(Mail.Template.PERSONAL_DATA, data),
            true
        )
    }
}

object Mail {

    data class MailData(
        val toMail: String,
        val toName: String,
        val subject: String,
        val body: String,
        val isHTML: Boolean = false
    )

    fun sendMailAsync(toMail: String, toName: String, subject: String, body: String, isHTML: Boolean = false) {
        //mailQueue.add(MailData(to, subject, body))
        Thread {
            sendMail(MailData(toMail, toName, subject, body, isHTML))
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
            if (data.isHTML)
                message.setContent(data.body, "text/html")
            else
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

    private fun resolver(): ITemplateResolver {
        return ClassLoaderTemplateResolver().apply {
            prefix = "templates/mail/"
            suffix = ".html"
            templateMode = TemplateMode.HTML
            characterEncoding = "UTF-8"
            isCacheable = false
        }
    }

    private fun engine(): TemplateEngine {
        val engine = TemplateEngine()
        engine.addDialect(LayoutDialect())
        engine.addTemplateResolver(resolver())
        return engine
    }

    enum class Template(val path: String) {
        OTP("otp"),
        USER_CREATION("user_creation"),
        USER_DEACTIVATION("user_deactivation"),
        USER_DELETION("user_deletion"),
        PERSONAL_DATA("personal_data")
    }

    private val defaultTemplateData: Map<String, Any> = mapOf(
        "applicationName" to "Expoll",
        "hostname" to config.loginLinkURL,
        "backendVersion" to config.serverVersion
    )

    fun fromTemplate(template: Template, data: Map<String, Any>): String {
        val context = org.thymeleaf.context.Context()
        val map = mutableMapOf<String, Any>()
        map.putAll(data)
        map.putAll(defaultTemplateData)
        context.setVariables(map)
        return engine().process(template.path, context)
    }
}