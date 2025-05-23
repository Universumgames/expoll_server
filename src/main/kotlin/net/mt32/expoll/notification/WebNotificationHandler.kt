package net.mt32.expoll.notification

import net.mt32.expoll.config
import net.mt32.expoll.entities.notifications.WebNotificationDevice
import net.mt32.expoll.helper.defaultJSON
import nl.martijndwars.webpush.Encoding
import nl.martijndwars.webpush.Notification
import nl.martijndwars.webpush.PushAsyncService
import nl.martijndwars.webpush.Urgency
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

data class WebNotification(val payload: String, val endpoint: String, val auth: String, val p256dh: String)

object WebNotificationHandler : NotificationHandler<WebNotificationDevice> {

    private val pushService: PushAsyncService

    private val sendingThread: Thread
    private val notificationQueue: MutableList<WebNotification>

    /*private val privateKey: ECPrivateKey
    private val publicKey: ECPublicKey

    private var client = HttpClient(Java) {
        engine {
            protocolVersion = java.net.http.HttpClient.Version.HTTP_1_1
        }
        install(ContentNegotiation) {
            json(defaultJSON)
        }
    }*/

    init {
        Security.addProvider(BouncyCastleProvider())
        pushService = PushAsyncService(
            config.notifications.publicApplicationServerKey,
            config.notifications.privateApplicationServerKey,
            config.notifications.webPushSubject
        )

        //privateKey = loadECPrivateKeyFromFile(config.notifications.privateApplicationServerKey) as ECPrivateKey
        //spublicKey = loadECPublicKeyFromFile(config.notifications.publicApplicationServerKey) as ECPublicKey

        notificationQueue = mutableListOf()
        sendingThread = Thread {
            while (true) {
                val notification = notificationQueue.removeFirstOrNull()
                if (notification != null) sendWebNotification(notification)
                else Thread.sleep(500)
            }
        }
        sendingThread.start()
    }

    private fun sendWebNotification(notification: WebNotification) {
        try {
            pushService.send(
                Notification(
                    notification.endpoint,
                    notification.p256dh,
                    notification.auth,
                    notification.payload,
                    Urgency.HIGH
                ),
                Encoding.AES128GCM
            )
        } catch (e: Exception) {
            println("Error while sending web notification: ${e.message}")
        }

        /*
        // https://blog.mozilla.org/services/2016/08/23/sending-vapid-identified-webpush-notifications-via-mozillas-push-service/
        val audience = notification.endpoint.substring(0, notification.endpoint.indexOf('/', 8))
        val jwt = JWT.create()
            .withSubject(config.notifications.webPushSubject)
            .withAudience(audience)
            .withExpiresAt(UnixTimestamp.now().addDays(7).toDate())
            .sign(Algorithm.ECDSA256(publicKey, privateKey))
        val receiverKey = notification.p256dh + "="


        async {
            client.request {
                Url(config.notifications.webPushSubject)
                method = HttpMethod.Post
                headers {
                    append("Authorization", "vapid t=$jwt,k=${publicKey.encoded.toBase64URL()}")
                }
            }
        }*/
    }

    override fun sendNotification(notification: UniversalNotification, device: WebNotificationDevice) {
        val payload = defaultJSON.encodeToString(notification.asSerializableNotification())
        val notification = WebNotification(
            payload,
            device.endpoint,
            device.auth,
            device.p256dh
        )
        notificationQueue.add(
            notification
        )
    }
}