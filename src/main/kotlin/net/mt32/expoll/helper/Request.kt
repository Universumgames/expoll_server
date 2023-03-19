package net.mt32.expoll.helper

import io.ktor.server.application.*
import io.ktor.server.request.*
import kotlinx.serialization.json.jsonObject
import net.mt32.expoll.auth.oldCookieName
import java.net.URLDecoder

@Deprecated(
    "Don't use this Method anymore, use the ktor serializer instead for serialising the request body",
    ReplaceWith("ktor serializer and call.receive()")
)
suspend fun getDataFromAny(call: ApplicationCall, key: String): String? {
    val request = call.request
    var cookie = request.cookies[oldCookieName]
    if (cookie != null) {
        if (cookie.startsWith("j:")) cookie = URLDecoder.decode(cookie.substring(2), "UTF-8")
        if (cookie != null) {
            val cookieVal = defaultJSON.parseToJsonElement(cookie).jsonObject[key]
            if (cookieVal != null) return cookieVal.toString().replace("\"", "")
                .removeNullString()
        }
    }
    val query = request.queryParameters[key]
    if (query != null)
        return query.removeNullString()
    val form = try {
        call.receiveParameters()[key]
    } catch (e: Exception) {
        null
    }
    if (form != null)
        return form.removeNullString()
    val body = if (call.receiveText()
            .isNotEmpty()
    ) defaultJSON.parseToJsonElement(call.receiveText()).jsonObject.toMap()[key].toString().replace("\"", "") else null
    if (body != null)
        return body.removeNullString()
    return null
}

@JvmName("getDataFromAnyOnObj")
@Deprecated(
    "Don't use this Method anymore, use the ktor serializer instead for serialising the request body",
    ReplaceWith("ktor serializer and call.receive()")
)
suspend fun ApplicationCall.getDataFromAny(key: String): String? =
    getDataFromAny(this, key)
