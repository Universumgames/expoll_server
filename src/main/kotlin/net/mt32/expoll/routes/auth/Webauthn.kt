package net.mt32.expoll.routes.auth

import com.yubico.webauthn.*
import com.yubico.webauthn.data.*
import com.yubico.webauthn.data.ByteArray
import com.yubico.webauthn.exception.AssertionFailedException
import com.yubico.webauthn.exception.RegistrationFailedException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.mt32.expoll.auth.JWTSessionPrincipal
import net.mt32.expoll.auth.WebauthnRegistrationStorage
import net.mt32.expoll.auth.normalAuth
import net.mt32.expoll.commons.helper.ReturnCode
import net.mt32.expoll.commons.helper.UnixTimestamp
import net.mt32.expoll.commons.serializable.responses.SimpleAuthenticator
import net.mt32.expoll.commons.serializable.responses.SimpleAuthenticatorList
import net.mt32.expoll.commons.tUserID
import net.mt32.expoll.config
import net.mt32.expoll.entities.Authenticator
import net.mt32.expoll.entities.User
import net.mt32.expoll.helper.getDataFromAny
import net.mt32.expoll.helper.respondWithOTPJSON

fun Route.webauthnRoutes() {
    route("webauthn") {
        authenticate(normalAuth) {
            route("/register") {
                get {
                    registerInit(call)
                }
                post {
                    registerResponse(call)
                }
            }
            get("/list") {
                getAuthenticatorList(call)
            }
            post("/edit") {
                editAuthenticator(call)
            }
            delete {
                deleteAuthenticator(call)
            }
        }
        route("authenticate") {
            get {
                authInit(call)
            }
            post {
                authRes(call)
            }
        }
    }
}

val rpIdentity: RelyingPartyIdentity
    get() = RelyingPartyIdentity.builder().id(config.webauthn.rpID).name(config.webauthn.rpName).build()

val rp: RelyingParty
    get() = RelyingParty.builder().identity(rpIdentity).credentialRepository(WebauthnRegistrationStorage)
        .origins(setOf(config.webauthn.origin, config.webauthn.origin.replace("http:", "https:"))).build()

val registrationStorage: MutableMap<tUserID, PublicKeyCredentialCreationOptions> = mutableMapOf()

private suspend fun registerInit(call: ApplicationCall) {
    val principal = call.principal<JWTSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.UNAUTHORIZED)
        return
    }
    val user = principal.user

    val uID = UserIdentity.builder().name(user.username).displayName(user.username).id(
        ByteArray(user.id.toByteArray())
    ).build()
    val options = StartRegistrationOptions.builder().user(uID).build()
    val request = rp.startRegistration(options)

    registrationStorage[user.id] = request

    call.respondText(request.toCredentialsCreateJson(), ContentType.Application.Json)
}

private suspend fun registerResponse(call: ApplicationCall) {
    val principal = call.principal<JWTSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.UNAUTHORIZED)
        return
    }
    val user = principal.user
    val request = registrationStorage[user.id]
    if (request == null) {
        call.respond(ReturnCode.BAD_REQUEST)
        return
    }

    val publicKeyCredentialJson = call.receiveText()
    val pkc = PublicKeyCredential.parseRegistrationResponseJson(publicKeyCredentialJson)

    try {
        val result = rp.finishRegistration(
            FinishRegistrationOptions.builder()
                .request(request)
                .response(pkc)
                .build()
        )
        val auth = Authenticator(
            user.id,
            user.username,
            result.keyId.id.base64,
            result.publicKeyCose.base64,
            result.signatureCount.toInt(),
            result.keyId.transports.get().map { it.id },
            call.request.header("user-agent") ?: "name",
            call.request.header("user-agent") ?: "",
            UnixTimestamp.now()
        )
        auth.save()
        call.respondText("{\"verified\":true}", ContentType.Application.Json)
    } catch (e: RegistrationFailedException) {
        e.printStackTrace()
        call.respond(ReturnCode.INTERNAL_SERVER_ERROR)
    }
}

//val authStorage: MutableMap<tUserID, AssertionRequest> = mutableMapOf()
val challengeStorage: MutableMap<ByteArray, AssertionRequest> = mutableMapOf()
private suspend fun authInit(call: ApplicationCall) {
    val username = call.getDataFromAny("username")
    val mail = call.getDataFromAny("mail")
    if (username == null && mail == null) {
        call.respond(ReturnCode.MISSING_PARAMS)
        return
    }
    val user = username?.let { User.byUsername(it) } ?: mail?.let { User.byMail(it) }
    if (user == null || !user.loginAble) {
        call.respond(ReturnCode.BAD_REQUEST)
        return
    }

    val request = rp.startAssertion(
        StartAssertionOptions.builder()
            .username(username)
            .build()
    )
    //authStorage[user.id] = request
    challengeStorage[request.publicKeyCredentialRequestOptions.challenge] = request
    call.respond(request.toCredentialsGetJson())
}

private suspend fun authRes(call: ApplicationCall) {
    val username = call.getDataFromAny("username")
    val mail = call.getDataFromAny("mail")
    val publicKeyCredentialJson = call.receiveText()
    val pkc = PublicKeyCredential.parseAssertionResponseJson(publicKeyCredentialJson)

    //val request = authStorage[user.id]
    val request = challengeStorage[pkc.response.clientData.challenge]
    if (request == null) {
        call.respond(ReturnCode.BAD_REQUEST)
        return
    }

    try {
        val result = rp.finishAssertion(
            FinishAssertionOptions.builder()
                .request(request) // The PublicKeyCredentialRequestOptions from startAssertion above
                .response(pkc)
                .build()
        )
        if (result.isSuccess) {
            val user = result.credential.userHandle.let { User.fromUserHandle(it) } ?: return
            call.respondWithOTPJSON(user, isNewUser = false)
        }
    } catch (e: AssertionFailedException) {
        e.printStackTrace()
        call.respond(ReturnCode.UNAUTHORIZED)
    }
}

private suspend fun getAuthenticatorList(call: ApplicationCall) {
    val principal = call.principal<JWTSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.UNAUTHORIZED)
        return
    }
    val simpleAuths = Authenticator.fromUser(principal.userID).map {
        SimpleAuthenticator(
            it.credentialID,
            it.name,
            it.initiatorPlatform,
            it.createdTimestamp.toClient()
        )
    }
    call.respond(SimpleAuthenticatorList(simpleAuths))
}

private suspend fun editAuthenticator(call: ApplicationCall) {
    val principal = call.principal<JWTSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.UNAUTHORIZED)
        return
    }
    val credentialID = call.getDataFromAny("credentialID")
    val newName = call.getDataFromAny("newName")
    if (credentialID == null || newName == null) {
        call.respond(ReturnCode.MISSING_PARAMS)
        return
    }
    val auth = Authenticator.fromCredentialID(credentialID)
    if (auth == null) {
        call.respond(ReturnCode.CHANGE_NOT_ALLOWED)
        return
    }
    auth.name = newName
    auth.save()
    call.respond(ReturnCode.OK)
}

private suspend fun deleteAuthenticator(call: ApplicationCall) {
    val principal = call.principal<JWTSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.UNAUTHORIZED)
        return
    }
    val credentialID = call.getDataFromAny("credentialID")
    if (credentialID == null) {
        call.respond(ReturnCode.MISSING_PARAMS)
        return
    }
    val auth = Authenticator.fromCredentialID(credentialID)
    if (auth == null) {
        call.respond(ReturnCode.CHANGE_NOT_ALLOWED)
        return
    }
    auth.delete()
    call.respond(ReturnCode.OK)
}