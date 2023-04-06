package net.mt32.expoll

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import net.mt32.expoll.helper.defaultJSON
import net.mt32.expoll.helper.mergeJsonObjects
import java.io.File

@Serializable
data class MailConfig(
    val mailServer: String = "",
    val mailPort: Int = 0,
    val mailSecure: Boolean = false,
    val mailUser: String = "",
    val mailPassword: String = ""
)

@Serializable
data class DatabaseConfig(
    val type: String = "",
    val host: String = "",
    val port: Int = 0,
    val rootPW: String = ""
)

@Serializable
data class WebauthnConfig(
    val rpName: String = "",
    val rpID: String = "",
    val origin: String = ""
)

@Serializable
data class NotificationConfig(
    val bundleID: String = "",
    val teamID: String = "",
    val apnsKeyID: String = "",
    val apnsKeyPath: String = "",
    val apnsURL: String = ""
)

@Serializable
data class TestUserConfig(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val username: String = "",
    val otp: String = ""
)

@Serializable
data class CompatibleVersionDescriptor(
    val from: String? = null,
    val to: String? = null,
    val exact: String? = null
)

@Serializable
data class JWTConfig(
    val secret: String = "",
    val issuer: String = "",
    val audience: String = "",
    val realm: String = ""
)

@Serializable
data class OIDCIDPConfig(
    val clientID: String,
    val clientSecret: String? = null,
    val privateKeyPath: String? = null,
    val privateKeyID: String? = null,
    val audience: String? = null,
    val redirectURL: String,
    val discoveryURL: String
)

@Serializable
data class OIDCConfig(
    val idps: Map<String, OIDCIDPConfig> = mapOf()
)

@Serializable
data class ConfigData(
    val mail: MailConfig = MailConfig(),
    val serverPort: Int = 0,
    val frontEndPort: Int = 0,
    val loginLinkURL: String = "",
    val superAdminMail: String = "",
    val database: DatabaseConfig = DatabaseConfig(),
    val maxPollCountPerUser: Int = 0,
    val recaptchaAPIKey: String = "",
    val serverVersion: String = "",
    val webauthn: WebauthnConfig = WebauthnConfig(),
    val shareURLPrefix: String = "",
    val notifications: NotificationConfig = NotificationConfig(),
    val testUser: TestUserConfig = TestUserConfig(),
    val minimumRequiredClientVersion: String = "",
    val compatibleVersions: List<CompatibleVersionDescriptor> = listOf(),
    val jwt: JWTConfig = JWTConfig(),
    val oidc: OIDCConfig = OIDCConfig(),
    val cookieDomain: String = ""
)

var config: ConfigData = ConfigData()

object ConfigLoader {

    fun load(environment: String = "") {
        val defaultConfigFile = File("config/default.json").readText()
        val desiredConfigFile = File("config/${environment}.json").readText()
        val defaultConf: JsonObject = defaultJSON.decodeFromString(defaultConfigFile)
        val desiredConfig: JsonObject = defaultJSON.decodeFromString(desiredConfigFile)

        val merged = mergeJsonObjects(defaultConf, desiredConfig)
        config = defaultJSON.decodeFromJsonElement(merged)

        println("Loaded config with")
        println(defaultJSON.encodeToString(config))
    }
}