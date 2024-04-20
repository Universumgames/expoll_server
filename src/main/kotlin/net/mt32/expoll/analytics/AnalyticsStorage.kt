package net.mt32.expoll.analytics

import net.mt32.expoll.helper.UnixTimestamp
import net.mt32.expoll.helper.getDelayToMidnight
import net.mt32.expoll.notification.ExpollNotificationHandler
import java.util.*

object AnalyticsStorage {
    private val timer: Timer = Timer()

    var requestCountStorage: MutableMap<String, RequestCountStorageElement> = mutableMapOf()
    var requestResponseDurations: MutableMap<RequestDurationKey, MutableList<Long>> = mutableMapOf()
    var notificationCount: MutableMap<ExpollNotificationHandler.ExpollNotification, Long>
    var lastReset: UnixTimestamp

    init {
        val now = Calendar.getInstance()
        val delay = getDelayToMidnight(now)
        timer.schedule(object : TimerTask() {
            override fun run() {
                resetStatistics()
            }
        }, delay, UnixTimestamp.zero().addDays(1).millisSince1970)
        lastReset = UnixTimestamp.now()
        notificationCount = mutableMapOf()
    }

    private fun resetStatistics(){
        requestCountStorage = mutableMapOf()
        requestResponseDurations = mutableMapOf()
        notificationCount = mutableMapOf()
        lastReset = UnixTimestamp.now()
    }

}