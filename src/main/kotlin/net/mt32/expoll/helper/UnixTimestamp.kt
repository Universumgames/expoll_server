package net.mt32.expoll.helper

import net.mt32.expoll.tClientDateTime
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*


class UnixTimestamp private constructor() : Comparable<UnixTimestamp> {

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

    override operator fun compareTo(other: UnixTimestamp): Int {
        val diff = millisSince1970 - other.millisSince1970
        return when {
            diff < 0 -> -1
            diff > 0 -> 1
            else -> 0
        }
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
        return Date.from(Instant.ofEpochMilli(millisSince1970))
    }

    fun asSecondsSince1970(): Long {
        return secondsSince1970
    }

    fun asMillisSince1970(): Long {
        return millisSince1970
    }

    fun toDB(): Long {
        return secondsSince1970
    }

    fun toClient(): tClientDateTime {
        return millisSince1970
    }

    fun toUnixEpoch(): Long {
        return secondsSince1970
    }

    fun nextMidnight(): UnixTimestamp {
        val cal = Calendar.getInstance()
        cal.time = toDate()
        cal.add(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        return UnixTimestamp.fromDate(cal.time)
    }

    fun todaysMidnight(): UnixTimestamp {
        val cal = Calendar.getInstance()
        cal.time = toDate()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        return UnixTimestamp.fromDate(cal.time)
    }

    override fun toString(): String {
        return toDate().toString()
    }

    fun toDateString(): String {
        val tz = TimeZone.getTimeZone("UTC")
        val df: DateFormat = with(SimpleDateFormat("yyyy-MM-dd")) {
            timeZone = tz
            this
        }
        return df.format(toDate())
    }

    fun toDateTimeString(includeSeconds: Boolean = false): String {
        val tz = TimeZone.getTimeZone("UTC")
        val df: DateFormat = with(SimpleDateFormat("yyyy-MM-dd HH:mm" + if (includeSeconds) ":ss" else "")) {
            timeZone = tz
            this
        }
        return df.format(toDate())
    }

    companion object {
        fun now(): UnixTimestamp {
            return DateTime.now(DateTimeZone.UTC).toDate().toUnixTimestamp()
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

        fun fromDateTimeComponents(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): UnixTimestamp {
            val cal = Calendar.getInstance()
            cal.set(year, month - 1, day, hour, minute, second)
            return fromDate(cal.time)
        }

        fun fromDateComponents(year: Int, month: Int, day: Int): UnixTimestamp {
            return fromDateTimeComponents(year, month, day, 0, 0, 0)
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