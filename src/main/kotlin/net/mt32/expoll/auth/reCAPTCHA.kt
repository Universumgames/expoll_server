package net.mt32.expoll.auth

import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import net.mt32.expoll.config
import net.mt32.expoll.helper.defaultJSON

private val client = HttpClient(Java)
@OptIn(InternalAPI::class)
suspend fun verifyGoogleCAPTCHA(token: String): reCAPTCHAResponse {
    val response = client.post("https://www.google.com/recaptcha/api/siteverify"){
        /*body = """
        {
            "params": {
                "secret": "${config.recaptchaAPIKey}",
                "response": "$token"
            }
        }
        """.trimIndent()*/

        parameter("secret", config.recaptchaAPIKey)
        parameter("response", token)
        contentType(ContentType.Application.Json)
    }
    val responseString = response.bodyAsText()
    return defaultJSON.decodeFromString(responseString)
}

@Serializable
data class reCAPTCHAResponse(
    val success: Boolean,
    val score: Double,
    val action: String,
    val challenge_ts: String,
    val hostname: String
)