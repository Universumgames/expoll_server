package net.mt32.expoll.notification

import kotlinx.serialization.encodeToString
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

    init {
        Security.addProvider(BouncyCastleProvider())
        pushService = PushAsyncService(
            config.notifications.publicApplicationServerKey,
            config.notifications.privateApplicationServerKey,
            "mailto:programming@universegame.de"
        )

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
        try{
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
        }catch (e:Exception){
            println("Error while sending web notification: ${e.message}")
        }
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