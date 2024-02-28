package net.mt32.expoll.auth

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import net.mt32.expoll.helper.defaultJSON
import java.io.FileInputStream
import java.security.cert.CertificateFactory


// TODO test functionality
@OptIn(ExperimentalSerializationApi::class)
fun verifyAppAttest(attestBase64: String): Boolean {
    return true
    val attestJSON = String(java.util.Base64.getDecoder().decode(attestBase64))
    val attestCBOR: AppleAppAttest = Cbor.decodeFromByteArray(java.util.Base64.getDecoder().decode(attestBase64))
    print(attestCBOR)
    val attest: AppleAppAttest = defaultJSON.decodeFromString(attestJSON)

    val fact: CertificateFactory = CertificateFactory.getInstance("X.509")
    val rootIS = FileInputStream("config/Apple_App_Attestation_Root_CA.pem")
    val rootCert = fact.generateCertificate(rootIS).publicKey
    rootIS.close()

    val credCert = fact.generateCertificate(attest.attStmt.x5c[0].inputStream())
    val caCert = fact.generateCertificate(attest.attStmt.x5c[1].inputStream())

    try {
        credCert.verify(caCert.publicKey)
        caCert.verify(rootCert)
    } catch (e: Exception) {
        return false
    }
    return true
}

@Serializable
data class AppleAppAttest(
    val fmt: String,
    val attStmt: AttestStatement,
    val authData: ByteArray,
    //val signature: ByteArray
)

@Serializable
data class AttestStatement(
    val x5c: List<ByteArray>,
    val receipt: ByteArray
)