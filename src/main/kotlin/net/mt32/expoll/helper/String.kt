package net.mt32.expoll.helper

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