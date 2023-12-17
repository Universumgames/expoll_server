package net.mt32.expoll.helper

object Hash{
    fun md5(input: String): String {
        val md = java.security.MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}