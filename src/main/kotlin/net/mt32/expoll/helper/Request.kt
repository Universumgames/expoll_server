package net.mt32.expoll.helper

import io.ktor.server.application.*
import io.ktor.server.request.*
import kotlinx.serialization.json.jsonObject
import net.mt32.expoll.auth.cookieName
import java.net.URLDecoder

@Deprecated(
    "Don't use this Method anymore, use the ktor serializer instead for serialising the request body",
    ReplaceWith("ktor serializer and call.receive()")
)
suspend fun getDataFromAny(call: ApplicationCall, key: String): String? {
    val request = call.request
    var cookie = request.cookies[cookieName]
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
    var isForm = false
    val form = try {
        val params = call.receiveParameters()
        isForm = true
        params[key]
    } catch (e: Exception) {
        null
    }
    if (form != null)
        return form.removeNullString()
    val body = if (call.receiveText()
            .isNotEmpty() && !isForm
    ) defaultJSON.parseToJsonElement(call.receiveText()).jsonObject.toMap()[key].toString().replace("\"", "") else null
    if (body != null)
        return body.removeNullString()
    val header = request.headers[key]
    if(header != null)
        return header.removeNullString()
    return null
}

@JvmName("getDataFromAnyOnObj")
@Deprecated(
    "Don't use this Method anymore, use the ktor serializer instead for serialising the request body",
    ReplaceWith("ktor serializer and call.receive()")
)
suspend fun ApplicationCall.getDataFromAny(key: String): String? =
    getDataFromAny(this, key)

suspend fun ApplicationCall.anyParameter(key: String):String?
= parameters[key] ?: request.queryParameters[key] ?: receiveParameters()[key]

/*suspend fun ApplicationCall.respondRedirect(url: Url, body: String){
    response.headers.append("Location", url.toString())
    respond(HttpStatusCode.TemporaryRedirect, body)
}

suspend fun ApplicationCall.respondRedirect(url: String, body: String){
    response.headers.append("Location", url)
    respond(HttpStatusCode.TemporaryRedirect, body)
}*/