package net.mt32.expoll.notification

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.mt32.expoll.config
import net.mt32.expoll.entities.notifications.APNDevice
import net.mt32.expoll.helper.Hash
import net.mt32.expoll.helper.UnixTimestamp
import net.mt32.expoll.helper.async
import net.mt32.expoll.helper.defaultJSON
import net.mt32.expoll.security.loadECPrivateKeyFromFile
import net.mt32.expoll.tPollID
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.interfaces.ECPrivateKey
import java.security.spec.ECPrivateKeySpec
import java.util.*
import kotlin.math.min

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

object APNsNotificationHandler : NotificationHandler<APNDevice> {

    enum class APNStatusCodes(val value: Int) {
        NoErrors(200),
        BadDeviceToken(400),
        BadCollapseID(400),
        BadExpirationDate(400),
        BadMessageID(400),
        BadPriority(400),
        BadTopic(400),
        DeviceTokenNotForTopic(400),
        DuplicateHeaders(400),
        IdleTimeout(400),
        MissingDeviceToken(400),
        MissingTopic(400),
        PayloadEmpty(400),
        TopicDisallowed(400),
        BadCertificate(403),
        BadCertificateEnvironment(403),
        ExpiredProviderToken(403),
        Forbidden(403),
        InvalidProviderToken(403),
        MissingProviderToken(403),
        BadPath(404),
        MethodNotAllowed(405),
        Unregistered(410),
        PayloadTooLarge(413),
        TooManyProviderTokenUpdates(429),
        TooManyRequests(429),
        InternalServerError(500),
        ServiceUnavailable(503),
        Shutdown(503),
        Unknown(0);

        companion object {
            fun fromInt(value: Int): APNStatusCodes {
                return values().firstOrNull { it.value == value } ?: Unknown
            }
        }
    }

    @Serializable
    private data class APNResponseData(
        val reason: String,
        val timestamp: Long? = null
    )

    enum class APNStatus(val value: String, val isOK: Boolean = true) {
        BadDeviceToken("BadDeviceToken", false),
        OK("OK"),
        UNKNOWN_ERROR("UnknownError", false),
        UNKNOWN("");

        companion object {
            fun fromString(value: String): APNStatus? {
                return values().firstOrNull { it.value == value }
            }
        }

        override fun toString(): String {
            return value
        }
    }

    private var client = HttpClient(Java) {
        engine {
            protocolVersion = java.net.http.HttpClient.Version.HTTP_2
        }
        install(ContentNegotiation){
            json(defaultJSON)
        }
    }

    suspend fun getAPNSBearer(): String? {
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

    private var _apnsBearer: String? = null
    private var _apnsAge: Date? = null

    private val apnsKey: PrivateKey
    private val ecAPNsKey: ECPrivateKey?

    private val sendingThread: Thread

    private val apnQueue: MutableList<APNData>

    init {
        apnsKey = getAPNsKey()
        ecAPNsKey = apnsKey.toECPrivateKey()
        apnQueue = mutableListOf()
        sendingThread = async {
            while (true) {
                val apn = apnQueue.removeFirstOrNull()
                if (apn != null) runBlocking { sendAPN(apn) }
                else Thread.sleep(500)
            }
        }
    }


    private fun getAPNsKey(): PrivateKey {
        return loadECPrivateKeyFromFile(config.notifications.apnsKeyPath)!!
        //return getPrivateKey(config.notifications.apnsKeyPath, "EC")!!
    }



    private suspend fun createAPNSBearerToken(): String? {
        if (ecAPNsKey == null) return null
        return JWT.create()
            .withKeyId(config.notifications.apnsKeyID)
            .withIssuedAt(Date())
            .withIssuer(config.notifications.teamID)
            .sign(Algorithm.ECDSA256(null, ecAPNsKey))
    }

    private suspend fun sendAPN(data: APNData): APNStatus {
        val bearer = getAPNSBearer() ?: return APNStatus.UNKNOWN_ERROR

        val response = client.request(Url(config.notifications.apnsURL + "/3/device/${data.deviceToken}")) {
            method = HttpMethod.Post
            headers {
                append("scheme", "https")
                append("authorization", "bearer ${bearer}")
                //append("path", "/3/device/${deviceToken}")
                append("apns-push-type", data.pushType.value)
                append("apns-expiration", "0")
                append("apns-priority", data.priority.priority.toString())
                append("apns-topic", config.notifications.bundleID)
                if (data.collapseID != null)
                    append("apns-collapse-id", data.collapseID.substring(0, min(data.collapseID.length, 64)))
            }
            contentType(ContentType.Application.Json)
            setBody(defaultJSON.encodeToString(data.payload))
        }
        if (response.status.value in (400..499)) {
            val errorData: APNResponseData? = response.jsonBodyOrNull()
            if(errorData == null){
                println("Error while sending APN: ${response.status}")
                println(response.bodyAsText())
                return APNStatus.UNKNOWN_ERROR
            }
            val error = APNStatus.fromString(errorData.reason)
            print(errorData)
            println("Device: ${data.deviceToken}")
            return error ?: APNStatus.UNKNOWN_ERROR
        }
        return APNStatus.OK
    }

    private data class APNData(
        val deviceToken: String,
        val expiration: UnixTimestamp,
        val payload: IAPNsPayload,
        val priority: APNsPriority,
        val pushType: APNsPushType = APNsPushType.ALERT,
        val collapseID: String? = null
    )

    @Serializable
    @SerialName("expollPayload")
    @Deprecated("use normal APNsPayload instead after iOS App Version 3.3.0")
    class ExpollAPNsPayload(
        override val aps: APS,
        override val additionalData: Map<String, String>,
        val pollID: tPollID? = null
    ) : IAPNsPayload {
    }

    override fun sendNotification(notification: UniversalNotification, device: APNDevice) {
        val apnsNotification = APNsNotification(
            null,
            null,
            null,
            titleLocalisationKey = notification.title,
            titleLocalisationArgs = notification.titleArgs,
            bodyLocalisationKey = notification.body,
            bodyLocalisationArgs = notification.bodyArgs,
        )
        val payload = APNsPayload(APS(apnsNotification), notification.additionalData)
        apnQueue.add(
            APNData(
                device.deviceID,
                notification.expiration,
                payload,
                APNsPriority.medium,
                APNsPushType.ALERT,
                Hash.md5(notification.additionalData.values.toList().joinToString())
            )
        )
    }
}

private suspend inline fun <reified T> HttpResponse.bodyOrNull(): T? {
    try {
        return this.body()
    } catch (e: Exception) {
        return null
    }
}

private suspend inline fun <reified T> HttpResponse.jsonBodyOrNull(): T? {
    try {
        return defaultJSON.decodeFromString(this.bodyAsText())
    } catch (e: Exception) {
        return null
    }
}
