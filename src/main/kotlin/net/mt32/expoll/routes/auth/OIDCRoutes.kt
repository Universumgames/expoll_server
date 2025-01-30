package net.mt32.expoll.routes.auth

import com.auth0.jwt.JWT
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import io.ktor.utils.io.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import net.mt32.expoll.auth.JWTSessionPrincipal
import net.mt32.expoll.auth.OIDC
import net.mt32.expoll.auth.normalAuth
import net.mt32.expoll.entities.OIDCUserData
import net.mt32.expoll.entities.User
import net.mt32.expoll.helper.*
import net.mt32.expoll.tUserID
import java.util.*

private val client = HttpClient(Java) {
    install(ContentNegotiation) {
        json(defaultJSON)
    }
}

@OptIn(InternalAPI::class)
fun Route.oidcRoutes() {
    route("/oidc") {
        get("/providers") {
            listIDPs(call)
        }
        staticResources("/images", "static/oidc")
        authenticate(normalAuth) {
            get("/connections") {
                getConnections(call)
            }

            route("/addConnection") {
                for (idp in OIDC.data.values) {
                    get(idp.name) {
                        oidcLoginInit(call, idp)
                    }
                }
            }

            route("/removeConnection") {
                for (idp in OIDC.data.values) {
                    delete(idp.name) {
                        removeOIDCConnection(call, idp)
                    }
                }
            }

            for (idp in OIDC.data.values) {
                delete(idp.name) {
                    removeOIDCConnection(call, idp)
                }
            }
        }

        // not working
        // authenticate(normalAuth, strategy = AuthenticationStrategy.Optional) {
        for (idp in OIDC.data.values) {
            route(idp.name) {
                get {
                    if (call.parameters.isEmpty() || call.parameters["app"]?.isNotEmpty() == true) {
                        oidcLoginInit(call, idp)
                    } else {
                        oidcLogin(call, idp)
                    }
                }
                post {
                    try {
                        if (call.parameters.isEmpty() && call.receiveParameters().isEmpty()) {
                            oidcLoginInit(call, idp)
                        } else {
                            oidcLogin(call, idp)
                        }
                    } catch (e: ContentTransformationException) {
                        e.printStackTrace()
                        oidcLogin(call, idp)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        call.respond(ReturnCode.BAD_REQUEST)
                    }
                }
            }
        }
        //}
    }
}

@Serializable
data class OIDCInfo(
    val key: String,
    val imageURI: String,
    val iconFileName: String,
    val iconBackgroundColorHex: String,
    val textColorHex: String,
    val title: String
)

private suspend fun listIDPs(call: ApplicationCall) {
    call.respond(OIDC.data.map {
        OIDCInfo(
            it.value.name,
            it.value.oidcidpConfig.imageURI,
            it.value.oidcidpConfig.iconConfig.iconFileName,
            it.value.oidcidpConfig.iconConfig.backgroundColorHex,
            it.value.oidcidpConfig.iconConfig.textColorHex,
            it.value.oidcidpConfig.title,
        )
    })
}

private suspend fun getConnections(call: ApplicationCall) {
    val principal = call.principal<JWTSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.INTERNAL_SERVER_ERROR)
        return
    }
    val connections = OIDCUserData.byUser(principal.userID)
    call.respond(connections.map { it.toConnectionOverview() })
}

private interface State {
    val timestamp: UnixTimestamp
    val isApp: Boolean
}

private data class LoggedInState(
    override val timestamp: UnixTimestamp,
    val userID: tUserID,
    override val isApp: Boolean = false
) : State

private data class FreshState(
    override val timestamp: UnixTimestamp,
    override val isApp: Boolean
) : State

private val stateStorage: MutableMap<Long, State> = mutableMapOf()

private suspend fun oidcLoginInit(call: ApplicationCall, idp: OIDC.OIDCIDPData) {
    val scope = "openid email " + if (idp.metadata.scopesSupported.contains("profile")) "profile" else "name"
    val url = URLBuilder(idp.metadata.authorizationEndpoint)
    val toRemoveKeys = mutableListOf<Long>()
    stateStorage.forEach {
        if (it.value.timestamp.addHours(1) < UnixTimestamp.now())
            toRemoveKeys.add(it.key)
    }
    toRemoveKeys.forEach { stateStorage.remove(it) }
    val isApp = call.request.queryParameters["app"] == "1"
    val noRedirect = call.request.queryParameters["redirect"] == "0"
    url.set {
        parameters.append("client_id", idp.clientID)
        parameters.append("scope", scope)
        parameters.append("response_type", "code")
        parameters.append("redirect_uri", idp.redirectUrl)
        parameters.append("nonce", UUID.randomUUID().toString())
        parameters.append("response_mode", "form_post")
        val principal = call.principal<JWTSessionPrincipal>()
        val nonce = createNonce()
        parameters.append("state", nonce.toString())

        if (principal != null) {
            stateStorage[nonce] = LoggedInState(UnixTimestamp.now(), principal.userID, isApp)
        } else
            stateStorage[nonce] = FreshState(UnixTimestamp.now(), isApp)
    }
    if (noRedirect)
        call.respond(url.buildString())
    else call.respondRedirect(url.build())
}

@Serializable
data class OIDCUserParam(
    val email: String,
    val name: Name
) {
    @Serializable
    data class Name(
        val firstName: String,
        val lastName: String
    )
}

private suspend fun oidcLogin(call: ApplicationCall, idp: OIDC.OIDCIDPData) {
    val code = call.anyParameter("code")
    val userParam = call.anyParameter("user")
    val customIdentifier = call.anyParameter("id")?.removeNullString()?.replaceEmptyWithNull()
    val clientSecret = idp.clientSecret
    val response =
        client.submitForm(
            url = OIDC.data[idp.name]!!.metadata.tokenEndpoint,
            formParameters = Parameters.build {
                append("code", code!!)
                append("grant_type", "authorization_code")
                append("client_id", idp.oidcidpConfig.clientID)
                append("client_secret", clientSecret)
                append("redirect_uri", idp.redirectUrl)
            })/*client.post(OIDC.data[idp.name]!!.metadata.tokenEndpoint) {
        parameter("code", code)
        parameter("grant_type", "authorization_code")
        parameter("client_id", idp.config.clientID)
        parameter("client_secret", clientSecret)
        parameter("redirect_uri", idp.config.redirectURL)
    }*/

    val responseToken = response.body<OIDC.CodeResponse>()
    println(responseToken)
    val idToken: String = if (responseToken.error.isNotEmpty()) {
        val paramToken = call.anyParameter("token")
        if (paramToken == null) {
            call.respond(ReturnCode.INVALID_PARAMS)
            return
        }
        paramToken
    } else responseToken.idToken

    val jwt = JWT.decode(idToken)
    val decodedJWT = jwt.payload.decodeBase64String()
    val baseTokenData = defaultJSON.decodeFromString<OIDC.IDToken>(decodedJWT)
    val jwtHeader = defaultJSON.decodeFromString<OIDC.JWTHeader>(jwt.header.decodeBase64String())
    val tokenDataMap: Map<String, JsonElement> = defaultJSON.decodeFromString(decodedJWT)
    println(tokenDataMap)

    if (!OIDC.verifyIDToken(jwt, jwtHeader, baseTokenData, idp)) {
        call.respond(ReturnCode.BAD_REQUEST)
        return
    }

    val principal = call.principal<JWTSessionPrincipal>()
    val stateParam = call.anyParameter("state")?.toLong()
    val state = stateStorage[stateParam]
    stateStorage.remove(stateParam)
    if (principal == null && state is FreshState)
        loginUser(call, userParam, tokenDataMap, baseTokenData, idp, state)
    else if (state is LoggedInState) addOIDCConnection(
        call,
        userParam,
        tokenDataMap,
        baseTokenData,
        idp,
        principal,
        state
    )
    else call.respond(ReturnCode.INTERNAL_SERVER_ERROR)
}

private suspend fun addOIDCConnection(
    call: ApplicationCall,
    userParam: String?,
    tokenDataMap: Map<String, JsonElement>,
    baseTokenData: OIDC.IDToken,
    idp: OIDC.OIDCIDPData,
    principal: JWTSessionPrincipal?,
    state: LoggedInState
) {
    val parsedUser = userParam?.let { defaultJSON.decodeFromString<OIDCUserParam>(it) }
    val mailUse = parsedUser?.email ?: tokenDataMap["email"]?.jsonPrimitive?.contentOrNull
    ?: tokenDataMap["email"]?.jsonPrimitive?.contentOrNull
    val userID = principal?.userID ?: state.userID
    if (OIDCUserData.bySubjectAndIDP(baseTokenData.subject, idp.name) != null) {
        if (state.isApp)
            call.respondRedirect("expoll://reload")
        else call.respondRedirect("/")
        return
    }
    val oidcConnection = OIDCUserData(userID, idp.name, baseTokenData.issuer, baseTokenData.subject, mailUse)
    oidcConnection.save()
    if (state.isApp)
        call.respondRedirect("expoll://reload")
    else call.respondRedirect("/")
}

private suspend fun loginUser(
    call: ApplicationCall,
    userParam: String?,
    tokenDataMap: Map<String, JsonElement>,
    baseTokenData: OIDC.IDToken,
    idp: OIDC.OIDCIDPData,
    state: FreshState
) {
    val parsedUser = userParam?.let { defaultJSON.decodeFromString<OIDCUserParam>(it) }

    val mailUse = parsedUser?.email ?: tokenDataMap["email"]?.jsonPrimitive?.contentOrNull
    ?: tokenDataMap["email"]?.jsonPrimitive?.contentOrNull

    println(baseTokenData)
    // fetch user from db or create new
    val oidcUserData = OIDCUserData.bySubjectAndIDP(baseTokenData.subject, idp.name)
    if (oidcUserData == null && (mailUse == null) && parsedUser == null) {
        call.respond(ReturnCode.BAD_REQUEST)
        return
    }
    // check for user connection exists
    var user = oidcUserData?.let { User.loadFromID(it.userID) }
    if (user != null) {
        createAndRespondWithSession(call, user, state)
        return
    }
    // check for mail user
    user = (mailUse)?.let { User.byMail(it) }
    if (user != null) {
        // create connection to provider
        val oidcConnection = OIDCUserData(user.id, idp.name, baseTokenData.issuer, baseTokenData.subject, mailUse)
        oidcConnection.save()
        createAndRespondWithSession(call, user, state)
        return
    }

    // create user
    val firstNameUse =
        parsedUser?.name?.firstName ?: tokenDataMap["given_name"]?.jsonPrimitive?.contentOrNull
        ?: tokenDataMap["name"]?.jsonPrimitive?.contentOrNull
        ?: tokenDataMap["name"]?.jsonPrimitive?.contentOrNull
    val lastNameUse = parsedUser?.name?.lastName ?: tokenDataMap["family_name"]?.jsonPrimitive?.contentOrNull
    var userNameUse = tokenDataMap["preferred_username"]?.jsonPrimitive?.contentOrNull
        ?: tokenDataMap["nickname"]?.jsonPrimitive?.contentOrNull
    if (mailUse == null || firstNameUse == null) {
        call.respond(ReturnCode.BAD_REQUEST)
        return
    }
    userNameUse?.let {
        if(User.byUsername(it) != null) {
            userNameUse = null
        }
    }
    user = User.createUser(userNameUse, firstNameUse, lastNameUse ?: "", mailUse, admin = false)
    OIDCUserData(user.id, idp.name, baseTokenData.issuer, baseTokenData.subject, mailUse).save()
    createAndRespondWithSession(call, user, state, isNewUser = true)
}

private suspend fun createAndRespondWithSession(
    call: ApplicationCall,
    user: User,
    state: State,
    isNewUser: Boolean = false
) {
    call.respondWithOTPRedirect(user, forApp = state.isApp, isNewUser = isNewUser)
}

private suspend fun removeOIDCConnection(call: ApplicationCall, idp: OIDC.OIDCIDPData) {
    val principal = call.principal<JWTSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.INTERNAL_SERVER_ERROR)
        return
    }
    val connections = OIDCUserData.byUser(principal.userID)
    val connection = connections.firstOrNull { it.idpName == idp.name }
    if (connection == null) {
        call.respond(ReturnCode.BAD_REQUEST)
        return
    }
    connection.delete()
    call.respond(ReturnCode.OK)
}