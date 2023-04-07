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
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import net.mt32.expoll.auth.JWTSessionPrincipal
import net.mt32.expoll.auth.OIDC
import net.mt32.expoll.auth.normalAuth
import net.mt32.expoll.entities.OIDCUserData
import net.mt32.expoll.entities.User
import net.mt32.expoll.helper.*
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
        authenticate(normalAuth) {
            get("/connections") {
                getConnections(call)
            }
            route("/addConnection"){
                for (idp in OIDC.data.values) {
                    get(idp.name) {
                        oidcLoginInit(call, idp)
                    }
                }
            }
        }

        for (idp in OIDC.data.values) {
            route(idp.name) {
                get {
                    if (call.parameters.isEmpty()) {
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
    }
}

private suspend fun listIDPs(call: ApplicationCall) {
    call.respondText(
        contentType = ContentType.Application.Json,
        text = defaultJSON.encodeToString(OIDC.data.values.map { it.name }.filter { !it.contains("2") })
    )
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

val stateStorage: MutableMap<Long, Pair<UnixTimestamp, String>> = mutableMapOf()

private suspend fun oidcLoginInit(call: ApplicationCall, idp: OIDC.OIDCIDPData) {
    val scope = "openid email " + if (idp.metadata.scopesSupported.contains("profile")) "profile" else "name"
    val url = URLBuilder(idp.metadata.authorizationEndpoint)
    stateStorage.forEach {
        if (it.value.first.addHours(1) < UnixTimestamp.now())
            stateStorage.remove(it.key)
    }
    url.set {
        parameters.append("client_id", idp.config.clientID)
        parameters.append("scope", scope)
        parameters.append("response_type", "code")
        parameters.append("redirect_uri", idp.config.redirectURL)
        parameters.append("nonce", UUID.randomUUID().toString())
        parameters.append("response_mode", "form_post")
        val principal = call.principal<JWTSessionPrincipal>()
        if (principal != null) {
            val nonce = createNonce()
            stateStorage[nonce] = Pair(UnixTimestamp.now(), principal.userID)
            parameters.append("state", nonce.toString())
        }
    }
    call.respondRedirect(url.build())
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
                append("client_id", idp.config.clientID)
                append("client_secret", clientSecret)
                append("redirect_uri", idp.config.redirectURL)
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
    val tokenDataMap: Map<String, JsonPrimitive> = defaultJSON.decodeFromString(decodedJWT)
    println(tokenDataMap)

    if (!OIDC.verifyIDToken(jwt, jwtHeader, baseTokenData, idp)) {
        call.respond(ReturnCode.BAD_REQUEST)
        return
    }

    val principal = call.principal<JWTSessionPrincipal>()
    if (principal == null && call.anyParameter("state") == null)
        loginUser(call, userParam, tokenDataMap, baseTokenData, idp)
    else addOIDCConnection(call, userParam, tokenDataMap, baseTokenData, idp, principal)
}

private suspend fun addOIDCConnection(
    call: ApplicationCall,
    userParam: String?,
    tokenDataMap: Map<String, JsonPrimitive>,
    baseTokenData: OIDC.IDToken,
    idp: OIDC.OIDCIDPData,
    principal: JWTSessionPrincipal?
) {
    val parsedUser = userParam?.let { defaultJSON.decodeFromString<OIDCUserParam>(it) }
    val mailUse = parsedUser?.email ?: tokenDataMap["email"]?.contentOrNull ?: tokenDataMap["email"]?.contentOrNull
    val state = call.anyParameter("state")
    val userID = principal?.userID ?: stateStorage[state?.toLong()]?.second
    stateStorage.remove(state?.toLong())
    if (userID == null) {
        call.respond(ReturnCode.BAD_REQUEST)
        return
    }
    val oidcConnection = OIDCUserData(userID, idp.name, baseTokenData.issuer, baseTokenData.subject, mailUse)
    oidcConnection.save()
    call.respondRedirect("/")
}

private suspend fun loginUser(
    call: ApplicationCall,
    userParam: String?,
    tokenDataMap: Map<String, JsonPrimitive>,
    baseTokenData: OIDC.IDToken,
    idp: OIDC.OIDCIDPData
) {
    val parsedUser = userParam?.let { defaultJSON.decodeFromString<OIDCUserParam>(it) }

    val mailUse = parsedUser?.email ?: tokenDataMap["email"]?.contentOrNull ?: tokenDataMap["email"]?.contentOrNull

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
        createAndRespondWithSession(call, user)
        return
    }
    // check for mail user
    user = (mailUse)?.let { User.byMail(it) }
    if (user != null) {
        // create connection to provider
        val oidcConnection = OIDCUserData(user.id, idp.name, baseTokenData.issuer, baseTokenData.subject, mailUse)
        oidcConnection.save()
        createAndRespondWithSession(call, user)
        return
    }

    // create user
    val firstNameUse =
        parsedUser?.name?.firstName ?: tokenDataMap["given_name"]?.contentOrNull ?: tokenDataMap["name"]?.contentOrNull
        ?: tokenDataMap["name"]?.contentOrNull
    val lastNameUse = parsedUser?.name?.lastName ?: tokenDataMap["family_name"]?.contentOrNull
    var userNameUse = tokenDataMap["preferred_username"]?.contentOrNull ?: tokenDataMap["nickname"]?.contentOrNull
    if (mailUse == null || firstNameUse == null) {
        call.respond(ReturnCode.BAD_REQUEST)
        return
    }
    if (userNameUse == null || User.byUsername(userNameUse) != null) {
        userNameUse = idp.name + "_user_" + baseTokenData.subject.encodeBase64()
    }
    user = User(userNameUse, firstNameUse, lastNameUse ?: "", mailUse, admin = false)
    user.save()
    createAndRespondWithSession(call, user)
}

private suspend fun createAndRespondWithSession(call: ApplicationCall, user: User) {
    val otp = user.createOTP()
    call.respondRedirect(urlBuilder(call, otp.otp))
}