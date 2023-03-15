package net.mt32.expoll.helper

import java.util.*

fun ByteArray.toBase64(): String =
    String(Base64.getEncoder().encode(this))

fun String.base64ToByteArray(): ByteArray =
    Base64.getUrlDecoder().decode(this)