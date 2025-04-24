package net.mt32.expoll.helper

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import net.mt32.expoll.notification.APNsNotificationHandler
import net.mt32.expoll.notification.APNsPayload
import net.mt32.expoll.notification.IAPNsPayload

val defaultJSON = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    encodeDefaults = true
    serializersModule = SerializersModule {
        polymorphic(IAPNsPayload::class) {
            subclass(APNsNotificationHandler.ExpollAPNsPayload::class)
            subclass(APNsPayload::class)
        }
    }
}