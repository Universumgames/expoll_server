package net.mt32.expoll.helper

import io.ktor.server.application.*
import io.ktor.server.request.*
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.mt32.expoll.cookieName
import java.net.URLDecoder

suspend fun getDataFromAny(call: ApplicationCall, key: String): String? {
    val request = call.request
    var cookie = request.cookies[cookieName]
    if (cookie != null) {
        // TODO very error prone
        if (cookie.startsWith("j:")) cookie = URLDecoder.decode(cookie.substring(2), "UTF-8")
        if (cookie != null)
            return defaultJSON.parseToJsonElement(cookie).jsonObject[key].toString().replace("\"", "")
    }
    val query = request.queryParameters[key]
    if (query != null)
        return query
    val form = try {
        call.receiveParameters()[key]
    } catch (e: Exception) {
        null
    }
    if (form != null)
        return form
    val body = if (call.receiveText()
            .isNotEmpty()
    ) defaultJSON.parseToJsonElement(call.receiveText()).jsonObject[key]?.jsonPrimitive.toString() else null
    if (body != null)
        return body
    return null
}