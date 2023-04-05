package net.mt32.expoll.analytics

import net.mt32.expoll.helper.UnixTimestamp
import java.util.*

object AnalyticsStorage {
    private val timer: Timer = Timer()

    var requestCountStorage: MutableMap<String, RequestCountStorageElement> = mutableMapOf()

    init {
        val now = Calendar.getInstance()
        val delay = getDelayToMidnight(now)
        timer.schedule(object : TimerTask() {
            override fun run() {
                resetStatistics()
            }
        }, delay, UnixTimestamp.zero().addDays(1).millisSince1970)
    }

    private fun resetStatistics(){
        requestCountStorage.forEach { it.value.reset() }
    }

    private fun getDelayToMidnight(now: Calendar): Long {
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

}