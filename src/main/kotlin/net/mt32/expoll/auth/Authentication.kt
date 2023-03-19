package net.mt32.expoll.auth

import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import net.mt32.expoll.config
import net.mt32.expoll.entities.LoginKeySession
import net.mt32.expoll.entities.User
import net.mt32.expoll.helper.*
import net.mt32.expoll.tUserID
import org.jetbrains.exposed.sql.Join
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
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
    val loginKeySession: LoginKeySession,
    val user: User,
    val admin: Boolean,
    val superUser: Boolean
) : Principal

class UserAuthentication internal constructor(val authConfig: Config) : AuthenticationProvider(authConfig) {
    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val call = context.call

        call.startNewTiming("auth.gather", "Gather authentication data")

        val loginKey = getDataFromAny(call, "loginKey")
        if (loginKey == null) {
            call.respond(ReturnCode.UNAUTHORIZED)
            return
        }


        call.startNewTiming("auth.db", "Try getting data from db about session")
        val loginKeySessionUser = transaction {
            return@transaction Join(
                LoginKeySession, User,
                onColumn = LoginKeySession.userID, otherColumn = User.id,
                joinType = JoinType.INNER,
                additionalConstraint = { (LoginKeySession.userID eq User.id) and (LoginKeySession.loginKey eq loginKey) }
            ).selectAll().firstOrNull()
        }
        call.startNewTiming("auth.checks", "Check session validity")
        if (loginKeySessionUser == null) {
            call.respond(ReturnCode.UNAUTHORIZED)
            return
        }

        val userFound = User(loginKeySessionUser)
        val loginKeySession = LoginKeySession(loginKeySessionUser)
        if (loginKeySession.expirationTimestamp < Date().toUnixTimestamp()) {
            call.sessions.clear<ExpollCookie>()
            call.respond(ReturnCode.UNAUTHORIZED)
            return
        }

        call.startNewTiming("auth.cookie", "Set cookie and pass data for rest of request")

        call.sessions.set(ExpollCookie(loginKey, getDataFromAny(call, "originalLoginKey")))

        val superAdmin =
            userFound.mail.lowercase(Locale.getDefault()) == config.superAdminMail.lowercase(Locale.getDefault())
        val anyAdmin = userFound.admin || superAdmin

        if (authConfig.checkAdmin && !anyAdmin) {
            call.respond(ReturnCode.UNAUTHORIZED)
            return
        }

        context.principal(
            name,
            BasicSessionPrincipal(loginKey, userFound.id, loginKeySession, userFound, anyAdmin, superAdmin)
        )
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

@Serializable
data class PublicKeyCredential(
    val id: String,
    val rawId: ByteArray,
    val response: AuthenticatorAttestationResponse,
    val type: String
)

@Serializable
data class AuthenticatorAttestationResponse(
    val clientDataJSON: ByteArray,
    val attestationObject: ByteArray
)