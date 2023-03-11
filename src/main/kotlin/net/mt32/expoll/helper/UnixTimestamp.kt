package net.mt32.expoll.helper

import net.mt32.expoll.tClientDateTime
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


class UnixTimestamp(timestamp: Long) {

    var value: Long = timestamp

    constructor(date: Date) : this(date.time) {

    }

    operator fun plus(timestamp: UnixTimestamp): UnixTimestamp {
        return UnixTimestamp(value + timestamp.value)
    }

    operator fun plus(timestamp: Long): UnixTimestamp {
        return UnixTimestamp(value + timestamp)
    }

    operator fun plusAssign(timestamp: UnixTimestamp) {
        value += timestamp.value
    }

    operator fun plusAssign(timestamp: Long) {
        value += timestamp
    }

    operator fun minus(timestamp: UnixTimestamp): UnixTimestamp {
        return UnixTimestamp(value - timestamp.value)
    }

    operator fun minus(timestamp: Long): UnixTimestamp {
        return UnixTimestamp(value - timestamp)
    }

    operator fun minusAssign(timestamp: UnixTimestamp) {
        value -= timestamp.value
    }

    operator fun minusAssign(timestamp: Long) {
        value -= timestamp
    }

    fun equals(timestamp: UnixTimestamp): Boolean {
        return value == timestamp.value
    }

    operator fun compareTo(timestamp: UnixTimestamp): Int {
        return (value - timestamp.value).toInt()
    }

    fun addSeconds(seconds: Int): UnixTimestamp {
        value += seconds
        return this
    }

    fun addMinutes(minutes: Int): UnixTimestamp {
        return addSeconds(minutes * 60)
    }

    fun addHours(hours: Int): UnixTimestamp {
        return addMinutes(hours * 60)
    }

    fun addDays(days: Int): UnixTimestamp {
        return addHours(days * 24)
    }

    fun toDate(): Date {
        return dateFromUnixTimestamp(value)
    }

    fun toLong(): Long {
        return value
    }

    @Deprecated("Use toClient instead")
    fun toJSDate(): String {
        val tz = TimeZone.getTimeZone("UTC")
        val df: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'")
        return df.format(toDate())
    }

    fun toDB(): Long {
        return value
    }

    fun toClient(): tClientDateTime {
        return value * 1000
    }

    companion object {
        fun now(): UnixTimestamp {
            return Date().toUnixTimestamp()
        }
    }
}

fun Long.toUnixTimestamp(): UnixTimestamp {
    return UnixTimestamp(this)
}

fun String.toUnixTimestamp(): UnixTimestamp {
    val tz = TimeZone.getTimeZone("UTC")
    val df: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'")
    return df.parse(this).toUnixTimestamp()
}