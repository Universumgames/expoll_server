package net.mt32.expoll.helper

import java.text.SimpleDateFormat
import java.util.*

fun Date.toUnixTimestamp(): UnixTimestamp{
    return UnixTimestamp.fromDate(this)
}

fun dateFromUnixTimestamp(timestamp: Long): Date{
    return Date(timestamp * 1000)
}

fun timestampFromString(dbString: String): Long {
    return if(dbString.contains(":")) {
        val df: SimpleDateFormat = SimpleDateFormat("yyy-MM-dd HH:mm:ss")
        val date: Date = df.parse(dbString)
        date.time
    }else dbString.toLongOrNull() ?: 0
}