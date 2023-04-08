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

fun getDelayToMidnight(now: Calendar): Long {
    val midnight = Calendar.getInstance()
    midnight.set(Calendar.HOUR_OF_DAY, 0)
    midnight.set(Calendar.MINUTE, 0)
    midnight.set(Calendar.SECOND, 0)
    midnight.set(Calendar.MILLISECOND, 0)
    if (midnight.before(now)) {
        midnight.add(Calendar.DAY_OF_MONTH, 1)
    }
    return midnight.timeInMillis - now.timeInMillis
}