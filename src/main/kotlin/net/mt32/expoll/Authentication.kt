package net.mt32.expoll

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import net.mt32.expoll.entities.Session
import net.mt32.expoll.entities.User
import net.mt32.expoll.helper.getDataFromAny
import net.mt32.expoll.helper.toUnixTimestamp
import java.util.*

const val cookieName = "expoll_dat"

data class ExpollCookie(
    val loginKey: String,
    val originalLoginKey: String? = null
)

data class BasicSessionPrincipal(
    val loginKey: String,
    val userID: tUserID,
    val session: Session,
    val user: User
): Principal

class UserAuthentication internal constructor(config: Config): AuthenticationProvider(config){
    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val call = context.call
        val loginKey = getDataFromAny(call, "loginKey")
        if(loginKey == null) {
            call.respond(HttpStatusCode.BadRequest)
            return
        }

        val userFound = User.loadFromLoginKey(loginKey)
        if(userFound ==null) {
            call.respond(HttpStatusCode.Unauthorized)
            return
        }

        val session = Session.fromLoginKey(loginKey)
        if(session == null || session.expirationTimestamp < Date().toUnixTimestamp()){
            call.sessions.set(ExpollCookie(""))
            call.respond(HttpStatusCode.Unauthorized)
            return
        }

        context.principal(name, BasicSessionPrincipal(loginKey, userFound.id, session, userFound))
    }

    public class Config internal constructor(name: String?): AuthenticationProvider.Config(name){

    }
}

fun AuthenticationConfig.checkLoggedIn(
    name: String? = null,
    configure: UserAuthentication.Config.() -> Unit
){
    val provider = UserAuthentication(UserAuthentication.Config(name).apply(configure))
    register(provider)
}