package net.mt32.expoll.auth

import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import net.mt32.expoll.entities.Session
import net.mt32.expoll.entities.User
import net.mt32.expoll.helper.defaultJSON
import net.mt32.expoll.commons.tUserID

const val cookieName = "expoll_session"
val normalAuth: String? = null
const val adminAuth = "admin"

@Serializable
data class ExpollJWTCookie(
    val jwt: String,
    val originalJWT: String? = null
){
    companion object : SessionSerializer<ExpollJWTCookie> {
        override fun deserialize(text: String): ExpollJWTCookie {
            return defaultJSON.decodeFromString(text)
        }

        override fun serialize(session: ExpollJWTCookie): String {
            return defaultJSON.encodeToString(session)
        }
    }
}

data class JWTSessionPrincipal(
    val session: Session,
    val userID: tUserID,
    val user: User,
    val admin: Boolean,
    val superAdmin: Boolean,
    val originalUserID: tUserID?
){
    companion object {
        fun empty(): JWTSessionPrincipal {
            return JWTSessionPrincipal(
                Session.empty(),
                "-1",
                User.empty(),
                false,
                false,
                null
            )
        }
    }
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