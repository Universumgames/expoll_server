package net.mt32.expoll.notification

import io.github.nefilim.kjwt.*
import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString
import net.mt32.expoll.config
import net.mt32.expoll.helper.defaultJSON
import java.io.File
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.interfaces.ECPrivateKey
import java.security.spec.ECPrivateKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*

fun PrivateKey.toECPrivateKey(): ECPrivateKey? {
    var ecPrivateKey: ECPrivateKey? = null
    try {
        val keyFactory = KeyFactory.getInstance("EC")
        val ecPrivateKeySpec: ECPrivateKeySpec = keyFactory.getKeySpec(this, ECPrivateKeySpec::class.java)
        ecPrivateKey = keyFactory.generatePrivate(ecPrivateKeySpec) as ECPrivateKey
    } catch (e: Exception) {
        // Handle exception
    }
    return ecPrivateKey
}

fun <T : JWSAlgorithm> SignedJWT<T>.asToken(): String {
    return this.rendered
}

object APNsNotificationHandler {

    private var client = HttpClient(Java) {
        engine {
            protocolVersion = java.net.http.HttpClient.Version.HTTP_2
        }
    }

    suspend fun getAPNSBearer(): SignedJWT<JWSES256Algorithm>? {
        if (
            _apnsAge == null ||
            _apnsBearer == null ||
            // check that the bearer is not older than 40 minutes
            Date().getTime() - (_apnsAge ?: Date()).getTime() > 40 * 60 * 1000
        ) {
            _apnsBearer = createAPNSBearerToken()
            _apnsAge = Date()
        }
        return _apnsBearer
    }

    private var _apnsBearer: SignedJWT<JWSES256Algorithm>? = null
    private var _apnsAge: Date? = null

    private fun getAPNsKey(): PrivateKey {
        val file = File(config.notifications.apnsKeyPath)
        val content = file.readText().replace("-----BEGIN RSA PRIVATE KEY-----\n", "")
            .replace("-----END RSA PRIVATE KEY-----", "")
        val encoded = Base64.getDecoder().decode(content)
        return KeyFactory.getInstance("ES256").generatePrivate(PKCS8EncodedKeySpec(encoded))
    }

    private suspend fun createAPNSBearerToken(): SignedJWT<JWSES256Algorithm>? {
        val jwt = JWT.es256(JWTKeyID(config.notifications.apnsKeyID)) {
            issuedNow()
            issuer(config.notifications.teamID)
        }
        val ecKey = getAPNsKey().toECPrivateKey() ?: return null
        val signed = jwt.sign(ecKey)
        val signedOrNull = signed.fold(
            ifLeft = { null },
            ifRight = { it }
        )
        return signedOrNull
    }

    suspend fun sendAPN(
        deviceToken: String,
        expiration: Date,
        payload: APNsPayload,
        priority: APNsPriority,
        pushType: APNsPushType = APNsPushType.ALERT,
        collapseID: String?
    ) {
        val bearer = getAPNSBearer()?.asToken() ?: return

        val response = client.request {
            method = HttpMethod.Post
            headers {
                append(":scheme", "https")
                append(":method", "POST")
                append("authorization", "bearer ${bearer}")
                append(":path", "/3/device/${deviceToken}")
                append("apns-push-type", pushType.value)
                append("apns-expiration", "0")
                append("apns-priority", priority.priority.toString())
                append("apns-topic", config.notifications.bundleID)
                if (collapseID != null)
                    append("apns-collapse-id", collapseID.substring(0, 8))
            }
            contentType(ContentType.Application.Json)
            setBody(defaultJSON.encodeToString(payload))
        }
        print(response.bodyAsText())
    }
}