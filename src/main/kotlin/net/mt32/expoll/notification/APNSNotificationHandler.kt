package net.mt32.expoll.notification

import io.github.nefilim.kjwt.*
import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import net.mt32.expoll.config
import net.mt32.expoll.entities.notifications.APNDevice
import net.mt32.expoll.helper.UnixTimestamp
import net.mt32.expoll.helper.defaultJSON
import net.mt32.expoll.security.loadECKeyFile
import net.mt32.expoll.tPollID
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.interfaces.ECPrivateKey
import java.security.spec.ECPrivateKeySpec
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

object APNsNotificationHandler: NotificationHandler<APNDevice> {

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

    private val apnsKey: PrivateKey
    private val ecAPNsKey: ECPrivateKey?

    private val sendingThread: Thread

    private val apnQueue: MutableList<APNData>

    init {
        apnsKey = getAPNsKey()
        ecAPNsKey = apnsKey.toECPrivateKey()
        apnQueue = mutableListOf()
        sendingThread = Thread {
            while (true) {
                val apn = apnQueue.removeFirstOrNull()
                if (apn != null) runBlocking { sendAPN(apn) }
                else Thread.sleep(500)
            }
        }
        sendingThread.start()
    }


    private fun getAPNsKey(): PrivateKey {
        return loadECKeyFile(config.notifications.apnsKeyPath)!!
        //return getPrivateKey(config.notifications.apnsKeyPath, "EC")!!
    }

    private suspend fun createAPNSBearerToken(): SignedJWT<JWSES256Algorithm>? {
        if (ecAPNsKey == null) return null
        val jwt = JWT.es256(JWTKeyID(config.notifications.apnsKeyID)) {
            issuedNow()
            issuer(config.notifications.teamID)
        }
        val signed = jwt.sign(ecAPNsKey)
        val signedOrNull = signed.fold(
            ifLeft = { null },
            ifRight = { it }
        )
        return signedOrNull
    }

    @Deprecated("use sendNotification instead")
    fun sendAPN(
        deviceToken: String,
        expiration: UnixTimestamp,
        payload: IAPNsPayload,
        priority: APNsPriority,
        pushType: APNsPushType = APNsPushType.ALERT,
        collapseID: String? = null
    ) {
        apnQueue.add(APNData(deviceToken, expiration, payload, priority, pushType, collapseID))
    }

    private suspend fun sendAPN(data: APNData) {
        val bearer = getAPNSBearer()?.asToken() ?: return

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
                    append("apns-collapse-id", data.collapseID.substring(0, 8))
            }
            contentType(ContentType.Application.Json)
            setBody(defaultJSON.encodeToString(data.payload))
        }
        //println(response.bodyAsText())
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
    class ExpollAPNsPayload(
        override val aps: APS,
        val pollID: tPollID? = null
    ) : IAPNsPayload

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
        val pollID = notification.additionalData["pollID"]
        val payload = ExpollAPNsPayload(APS(apnsNotification), pollID)
        apnQueue.add(APNData(device.deviceID, notification.expiration, payload, APNsPriority.medium, APNsPushType.ALERT, pollID))
    }
}