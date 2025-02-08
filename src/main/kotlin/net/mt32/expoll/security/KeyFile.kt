package net.mt32.expoll.security

import java.io.File
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*

fun loadECPrivateKeyFromFile(path: String): PrivateKey? {
    val file = File(path)
    val content = String(file.readBytes()).replace("-----BEGIN PRIVATE KEY-----\n", "")
        .replace("-----END PRIVATE KEY-----", "").replace("\\s+".toRegex(), "")
    val encoded = Base64.getDecoder().decode(content)
    return KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(encoded))
}

fun loadECPublicKeyFromFile(path: String): PublicKey? {
    val file = File(path)
    val content = String(file.readBytes()).replace("-----BEGIN PUBLIC KEY-----\n", "")
        .replace("-----END PUBLIC KEY-----", "").replace("\\s+".toRegex(), "")
    val encoded = Base64.getDecoder().decode(content)
    return KeyFactory.getInstance("EC").generatePublic(PKCS8EncodedKeySpec(encoded))
}