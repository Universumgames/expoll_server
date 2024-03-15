package net.mt32.expoll.helper

import kotlinx.coroutines.runBlocking

fun async(block: suspend () -> Unit): Thread {
    val t = Thread {
        try {
            runBlocking {
                block()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    t.start()
    return t
}