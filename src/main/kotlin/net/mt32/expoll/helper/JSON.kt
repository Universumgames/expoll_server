package net.mt32.expoll.helper

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

val defaultJSON = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
}

/**
 * Merge two Json objects , if duplicates exist, use values from obj2
 */
fun mergeJsonObjects(obj1: JsonObject, obj2: JsonObject?): JsonObject {
    if (obj2 == null) return obj1
    val merged = obj1.toMutableMap();
    for (entry in merged.entries) {
        if (obj2.containsKey(entry.key)) {
            val isObj = merged[entry.key].toString().contains("{") || merged[entry.key].toString().contains("[")
            merged[entry.key] = if (isObj) mergeJsonObjects(
                entry.value.jsonObject,
                obj2[entry.key]?.jsonObject
            ) else obj2[entry.key]?.jsonPrimitive ?: entry.value
        }
    }
    for(entry in obj2.entries){
        if(merged.containsKey(entry.key)) continue
        merged.put(entry.key, entry.value)
    }
    return JsonObject(merged)
}