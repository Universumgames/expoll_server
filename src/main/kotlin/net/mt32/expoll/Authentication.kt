package net.mt32.expoll

import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import net.mt32.expoll.entities.Session
import net.mt32.expoll.entities.User
import net.mt32.expoll.helper.ReturnCode
import net.mt32.expoll.helper.defaultJSON
import net.mt32.expoll.helper.getDataFromAny
import net.mt32.expoll.helper.toUnixTimestamp
import java.util.*

const val cookieName = "expoll_dat"
val normalAuth: String? = null
const val adminAuth = "admin"

@Serializable
data class ExpollCookie(
    val loginKey: String,
    val originalLoginKey: String? = null
) {
    companion object : SessionSerializer<ExpollCookie> {
        override fun deserialize(text: String): ExpollCookie {
            return defaultJSON.decodeFromString(text)
        }

        override fun serialize(session: ExpollCookie): String {
            return defaultJSON.encodeToString(session)
        }
    }
}

data class BasicSessionPrincipal(
    val loginKey: String,
    val userID: tUserID,
    val session: Session,
    val user: User,
    val admin: Boolean,
    val superUser: Boolean
) : Principal

class UserAuthentication internal constructor(val authConfig: Config) : AuthenticationProvider(authConfig) {
    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val call = context.call
        val loginKey = getDataFromAny(call, "loginKey")
        if (loginKey == null) {
            call.respond(ReturnCode.UNAUTHORIZED)
            return
        }

        val userFound = User.loadFromLoginKey(loginKey)
        if (userFound == null) {
            call.respond(ReturnCode.UNAUTHORIZED)
            return
        }

        val session = Session.fromLoginKey(loginKey)
        if (session == null || session.expirationTimestamp < Date().toUnixTimestamp()) {
            call.sessions.set(ExpollCookie(""))
            call.respond(ReturnCode.UNAUTHORIZED)
            return
        }

        call.sessions.set(ExpollCookie(loginKey, getDataFromAny(call, "originalLoginKey")))

        val superAdmin =
            userFound.mail.lowercase(Locale.getDefault()) == config.superAdminMail.lowercase(Locale.getDefault())
        val anyAdmin = userFound.admin || superAdmin

        if (authConfig.checkAdmin && !anyAdmin) {
            call.respond(ReturnCode.UNAUTHORIZED)
            return
        }

        context.principal(name, BasicSessionPrincipal(loginKey, userFound.id, session, userFound, anyAdmin, superAdmin))
    }

    public class Config internal constructor(name: String?) : AuthenticationProvider.Config(name) {
        var checkAdmin = false
    }
}

fun AuthenticationConfig.checkLoggedIn(
    name: String? = null,
    configure: UserAuthentication.Config.() -> Unit
) {
    val provider = UserAuthentication(UserAuthentication.Config(name).apply(configure))
    register(provider)
}