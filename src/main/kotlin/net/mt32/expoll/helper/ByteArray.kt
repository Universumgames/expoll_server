package net.mt32.expoll.helper

import java.util.*
import kotlin.math.abs

fun ByteArray.toBase64(): String =
    String(Base64.getEncoder().encode(this))

fun ByteArray.toBase64URL(): String =
    String(Base64.getUrlEncoder().encode(this))

fun String.base64ToByteArray(): ByteArray =
    Base64.getUrlDecoder().decode(this)

fun ByteArray.toBase62(): String {
    val alphabet = String.Numbers + String.LowerEnglishAlphabet + String.UpperEnglishAlphabet
    val base = alphabet.length
    var str = ""
    for (b in this) {
        var value = abs(b.toInt())
        while (value > 0) {
            str += alphabet[value % base]
            value /= base
        }
    }
    return str
}