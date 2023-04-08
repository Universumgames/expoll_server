package net.mt32.expoll.helper

import net.mt32.expoll.tClientDateTime
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


class UnixTimestamp private constructor() {

    val secondsSince1970: Long
        get() = millisSince1970 / 1000

    var millisSince1970: Long = 0

    operator fun plus(timestamp: UnixTimestamp): UnixTimestamp {
        return fromMillisSince1970(millisSince1970 + timestamp.millisSince1970)
    }

    operator fun plusAssign(timestamp: UnixTimestamp) {
        millisSince1970 += timestamp.millisSince1970
    }

    operator fun plusAssign(timestamp: Long) {
        millisSince1970 += timestamp * 1000
    }

    operator fun minus(timestamp: UnixTimestamp): UnixTimestamp {
        return fromMillisSince1970(millisSince1970 - timestamp.millisSince1970)
    }

    operator fun minusAssign(timestamp: UnixTimestamp) {
        millisSince1970 -= timestamp.millisSince1970
    }


    fun equals(timestamp: UnixTimestamp): Boolean {
        return millisSince1970 == timestamp.millisSince1970
    }

    operator fun compareTo(timestamp: UnixTimestamp): Int {
        return (millisSince1970 - timestamp.millisSince1970).toInt()
    }

    fun addSeconds(seconds: Long): UnixTimestamp {
        millisSince1970 += seconds * 1000
        return this
    }

    fun addMinutes(minutes: Long): UnixTimestamp {
        return addSeconds(minutes * 60)
    }

    fun addHours(hours: Long): UnixTimestamp {
        return addMinutes(hours * 60)
    }

    fun addDays(days: Long): UnixTimestamp {
        return addHours(days * 24)
    }

    fun toDate(): Date {
        return Date(millisSince1970)
    }

    fun asSecondsSince1970(): Long {
        return secondsSince1970
    }

    fun asMillisSince1970(): Long {
        return millisSince1970
    }

    @Deprecated("Use toClient instead")
    fun toJSDate(): String {
        val tz = TimeZone.getTimeZone("UTC")
        val df: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'")
        return df.format(toDate())
    }

    fun toDB(): Long {
        return secondsSince1970
    }

    fun toClient(): tClientDateTime {
        return millisSince1970
    }

    companion object {
        fun now(): UnixTimestamp {
            return Date().toUnixTimestamp()
        }

        fun fromSecondsSince1970(seconds: Long): UnixTimestamp {
            val ts = UnixTimestamp()
            ts.millisSince1970 = seconds * 1000
            return ts
        }

        fun fromMillisSince1970(millis: Long): UnixTimestamp {
            val ts = UnixTimestamp()
            ts.millisSince1970 = millis
            return ts
        }

        fun fromDate(date: Date): UnixTimestamp {
            val ts = UnixTimestamp()
            ts.millisSince1970 = date.time
            return ts
        }

        fun zero(): UnixTimestamp{
            val ts = UnixTimestamp()
            ts.millisSince1970 = 0
            return ts
        }
    }
}

fun Long.toUnixTimestampAsSecondsSince1970(): UnixTimestamp {
    return UnixTimestamp.fromSecondsSince1970(this)
}

fun Long.toUnixTimestampFromDB(): UnixTimestamp {
    return toUnixTimestampAsSecondsSince1970()
}

fun Long.toUnixTimestampFromClient(): UnixTimestamp {
    return UnixTimestamp.fromMillisSince1970(this)
}

fun String.toUnixTimestampAsJSONDateString(): UnixTimestamp {
    val tz = TimeZone.getTimeZone("UTC")
    val df: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'")
    return df.parse(this).toUnixTimestamp()
}