package net.mt32.expoll.helper

import kotlinx.serialization.json.*
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
        polymorphic(IAPNsPayload::class){
            subclass(APNsNotificationHandler.ExpollAPNsPayload::class)
            subclass(APNsPayload::class)
        }
    }
}

/**
 * Merge two Json objects , if duplicates exist, use values from obj2
 */
fun mergeJsonObjects(obj1: JsonObject, obj2: JsonObject?): JsonObject {
    if (obj2 == null) return obj1
    val merged = obj1.toMutableMap();
    merged.forEach { entry ->
        if (obj2.containsKey(entry.key)) {
            val isObj = merged[entry.key].toString().contains("{") || merged[entry.key].toString().contains("[")
            merged[entry.key] = if (isObj) mergeJsonObjects(
                entry.value.jsonObject,
                obj2[entry.key]?.jsonObject
            ) else obj2[entry.key]?.jsonPrimitive ?: entry.value
        }
    }
    obj2.forEach { entry ->
        if (!merged.containsKey(entry.key))
            merged.put(entry.key, entry.value)
    }
    return JsonObject(merged)
}

fun JsonObject.toMap(): Map<String, *> = keys.asSequence().associateWith {
    when (val value = this[it])
    {
        is JsonArray ->
        {
            val map = (0 until value.size).associate { Pair(it.toString(), value[it]) }
            JsonObject(map).toMap().values.toList()
        }
        is JsonObject -> value.toMap()
        JsonNull -> null
        else            -> value
    }
}