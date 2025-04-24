package net.mt32.expoll.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.mt32.expoll.OIDCIDPConfig
import net.mt32.expoll.config
import net.mt32.expoll.commons.helper.UnixTimestamp
import net.mt32.expoll.helper.defaultJSON
import net.mt32.expoll.notification.toECPrivateKey
import net.mt32.expoll.security.loadECPrivateKeyFromFile
import java.math.BigInteger
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import java.time.Instant
import java.util.*


@Serializable
// see https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata
data class OIDCProviderMetadata(
    val issuer: String,
    @SerialName("authorization_endpoint") val authorizationEndpoint: String,
    @SerialName("token_endpoint") val tokenEndpoint: String,
    @SerialName("userinfo_endpoint") val userInfoEndpoint: String = "",
    @SerialName("jwks_uri") val jwksURI: String,
    @SerialName("registration_endpoint") val registrationEndpoint: String = "",
    @SerialName("scopes_supported") val scopesSupported: List<String> = listOf(),
    @SerialName("response_types_supported") val responseTypesSupported: List<String>,
    //....
    @SerialName("claims_supported") val claimsSupported: List<String> = listOf()
)

object OIDC {

    @Serializable
    data class OIDCIDPData(
        val name: String,
        val metadata: OIDCProviderMetadata,
        val oidcidpConfig: OIDCIDPConfig,
        val keys: List<Key>
    ) {
        val clientID: String
            get() = oidcidpConfig.clientID

        fun getVerifier(key: Key): JWTVerifier {
            val rsaPublicKeySpec = RSAPublicKeySpec(key.modulusInt, key.exponentInt)
            val keyFactory = KeyFactory.getInstance("RSA")
            val publicKey = keyFactory.generatePublic(rsaPublicKeySpec)
            val alg = Algorithm.RSA256(publicKey as RSAPublicKey?, null)
            return JWT.require(alg).withIssuer(metadata.issuer).build()
        }

        val clientSecret: String
            get() {
                return oidcidpConfig.clientSecret
                    ?: (runBlocking {
                        val key = loadECPrivateKeyFromFile(oidcidpConfig.privateKeyPath!!)!!
                        val keyID = oidcidpConfig.privateKeyID!!
                        return@runBlocking JWT.create()
                            .withKeyId(keyID)
                            .withIssuer(config.notifications.teamID)
                            .withIssuedAt(Instant.now())
                            .withExpiresAt(UnixTimestamp.now().addMinutes(90).toDate().toInstant())
                            .withAudience(oidcidpConfig.audience!!)
                            .withSubject(oidcidpConfig.clientID)
                            .sign(Algorithm.ECDSA256(key.toECPrivateKey()!!))
                    } ?: "")
            }

        fun getKey(kid: String): Key? {
            return keys.find { it.keyID == kid }
        }

        val redirectUrl: String
            get() = config.oidc.baseURL + "/auth/oidc/$name"

    }

    var data: MutableMap<String, OIDCIDPData> = mutableMapOf()

    suspend fun init() {
        val client = HttpClient(Java) {
            install(ContentNegotiation) {
                json(defaultJSON)
            }
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 5)
                delayMillis { retry -> 1000 }
            }
        }
        for (idp in config.oidc.idps.toSortedMap()) {
            try {
                print("Loading oidc config for ${idp.key}")
                val responseMetadata = client.get(idp.value.discoveryURL)
                print('.')
                val metadata = responseMetadata.body<OIDCProviderMetadata>()
                print('.')
                val responseKeys = client.get(metadata.jwksURI)
                print('.')
                val keys = responseKeys.body<KeyStorage>().keys
                print('.')
                val data = OIDCIDPData(idp.key, metadata, idp.value, keys)
                print('.')
                if (isValidIDP(data))
                    this.data[idp.key] = data
                println(" success")
            } catch (e: Exception) {
                println(" failed")
                e.printStackTrace()
            }
        }

        //println(defaultJSON.encodeToString(data))
    }

    private fun isValidIDP(data: OIDCIDPData): Boolean {
        return when {
            !data.metadata.scopesSupported.contains("name") && !data.metadata.scopesSupported.contains("profile") -> false
            !data.metadata.scopesSupported.contains("email") -> false
            !data.metadata.responseTypesSupported.contains("code") -> false
            data.oidcidpConfig.clientID.isEmpty() -> false
            else -> true
        }
    }

    fun verifyIDToken(jwt: DecodedJWT, header: JWTHeader, token: IDToken, idp: OIDCIDPData): Boolean {
        val key = idp.getKey(header.keyID)
        val verifier = key?.let { idp.getVerifier(it) }

        return when {
            !token.audience.equals(idp.oidcidpConfig.clientID, ignoreCase = true) -> false
            token.issuer != idp.metadata.issuer -> false
            verifier == null -> false
            key.algorithm != null && key.algorithm != header.algorithm -> false
            token.expirationTimestamp < UnixTimestamp.now() -> false
            token.issuedAtTimestamp > UnixTimestamp.now() -> false

            else -> true
        } && try {
            verifier?.verify(jwt)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    @Serializable
    data class CodeResponse(
        val error: String = "",
        @SerialName("error_description") val errorDescription: String = "",
        @SerialName("access_token") val accessToken: String = "",
        @SerialName("expires_in") val expiresIn: Long = -1,
        @SerialName("token_type") val tokenType: String = "",
        @SerialName("id_token") val idToken: String = ""
    )

    @Serializable
    /**
    see https://openid.net/specs/openid-connect-basic-1_0.html#IDToken
     */
    data class IDToken(
        @SerialName("iss") val issuer: String,
        @SerialName("sub") val subject: String,
        @SerialName("aud") val audience: String,
        @SerialName("exp") val expiration: Long,
        @SerialName("iat") val issuedAt: Long,
        @SerialName("auth_time") val authTime: Long? = null,
        @SerialName("nonce") val nonce: String? = null,
        @SerialName("at_hash") val atHash: String? = null,
        @SerialName("acr") val authContextClassReference: String? = null,
        @SerialName("amr") val authMethodsReference: String? = null,
        @SerialName("azp") val authorizedParty: String? = null
    ) {
        val expirationTimestamp: UnixTimestamp
            get() = UnixTimestamp.fromSecondsSince1970(expiration)

        val issuedAtTimestamp: UnixTimestamp
            get() = UnixTimestamp.fromSecondsSince1970(issuedAt)

        val authTimeTimestamp: UnixTimestamp?
            get() = authTime?.let { UnixTimestamp.fromSecondsSince1970(it) }
    }

    @Serializable
    data class JWTHeader(
        @SerialName("kid") val keyID: String,
        @SerialName("alg") val algorithm: String
    )

    @Serializable
    data class KeyStorage(
        val keys: List<Key>
    )

    @Serializable
    data class Key(
        @SerialName("kty") val keyType: String,
        @SerialName("kid") val keyID: String,
        @SerialName("use") val use: String,
        @SerialName("alg") val algorithm: String? = null,
        @SerialName("x5c") val x5cKeys: List<String> = listOf(),
        @SerialName("n") val modulus: String,
        @SerialName("e") val exponent: String,
        @SerialName("x5t") val thumbprint: String = ""
    ) {
        val modulusInt: BigInteger
            get() = BigInteger(1, Base64.getUrlDecoder().decode(modulus))

        val exponentInt: BigInteger
            get() = BigInteger(1, Base64.getUrlDecoder().decode(exponent))
    }
}
