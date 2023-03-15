package net.mt32.expoll.helper

fun String.removeNullString(): String? {
    if (this.equals("null", ignoreCase = true)) return null
    return this
}