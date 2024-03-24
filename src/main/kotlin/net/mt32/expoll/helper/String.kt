package net.mt32.expoll.helper

import io.ktor.util.*

fun String.removeNullString(): String? {
    if (this.equals("null", ignoreCase = true)) return null
    return this
}

fun String.replaceEmptyWithNull(): String?{
    if(this.isEmpty() || this.isBlank()) return null
    return this
}

fun String.getHostPartFromURL(): String?{
    val regex = Regex("^(?:https?:\\/\\/)?(?:[^@\\/\\n]+@)?(?:www\\.)?([^:\\/?\\n]+)")
    val matchResult = regex.find(this)
    return matchResult?.groupValues?.get(1)
}

val String.Companion.Numbers: String
    get() = "0123456789"

val String.Companion.LowerEnglishAlphabet: String
    get() = "abcdefghijklmnopqrstuvwxyz"

val String.Companion.UpperEnglishAlphabet: String
    get() = LowerEnglishAlphabet.toUpperCasePreservingASCIIRules()