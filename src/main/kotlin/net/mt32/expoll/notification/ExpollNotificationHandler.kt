package net.mt32.expoll.notification

import net.mt32.expoll.analytics.AnalyticsStorage
import net.mt32.expoll.config
import net.mt32.expoll.entities.Poll
import net.mt32.expoll.entities.User
import net.mt32.expoll.helper.UnixTimestamp


object ExpollNotificationHandler {

    var lastNotification: Triple<ExpollNotification, Poll?, User?> = Triple(ExpollNotification.EMPTY, null, null)
    var lastNotificationTime: UnixTimestamp = UnixTimestamp.zero()

    enum class ExpollNotification(val body: String, val title: String = "notification.poll.update %@") {
        EMPTY(""),
        STARTUP("notification.server.backend.update %@", "notification.server.backend.update.title"),
        VoteChange("notification.vote.change %@ %@"),
        UserAdded("notification.user.added %@ %@"),
        UserRemoved("notification.user.removed %@ %@"),
        PollDeleted("notification.poll.delete %@"),
        PollEdited("notification.poll.edited %@"),
        PollArchived("notification.poll.archived %@");

        fun getBodyArgs(user: User? = null, poll: Poll? = null): List<String> {
            val username = user?.let {
                it.username.substring(
                    0,
                    minOf(it.username.length, 20)
                ) + if (it.username.length > 20) "..." else ""
            } ?: ""
            return when (this) {
                STARTUP -> listOf(config.serverVersion)
                VoteChange -> listOf(username, poll?.name ?: "")
                UserAdded -> listOf(username, poll?.name ?: "")
                UserRemoved -> listOf(username, poll?.name ?: "")
                PollDeleted -> listOf(poll?.name ?: "")
                PollEdited -> listOf(poll?.name ?: "")
                PollArchived -> listOf(poll?.name ?: "")
                else -> listOf()
            }
        }

        fun getTitleArgs(user: User? = null, poll: Poll? = null): List<String> {
            return when (this) {
                STARTUP -> listOf()
                VoteChange -> listOf(poll?.name ?: "")
                UserAdded -> listOf(poll?.name ?: "")
                UserRemoved -> listOf(poll?.name ?: "")
                PollDeleted -> listOf(poll?.name ?: "")
                PollEdited -> listOf(poll?.name ?: "")
                PollArchived -> listOf(poll?.name ?: "")
                else -> listOf()
            }
        }

        fun isWantedByUser(user: User): Boolean {
            val notificationPreferences = user.notificationPreferences
            return when (this) {
                EMPTY -> false
                STARTUP -> user.admin
                VoteChange -> notificationPreferences.voteChange
                UserAdded -> notificationPreferences.userAdded
                UserRemoved -> notificationPreferences.userRemoved
                PollDeleted -> notificationPreferences.pollDeleted
                PollEdited -> notificationPreferences.pollEdited
                PollArchived -> notificationPreferences.pollArchived
            }
        }

        fun asUniversalNotification(affectedPoll: Poll? = null, affectedUser: User? = null): UniversalNotification {
            return UniversalNotification(
                title,
                body,
                titleArgs = getTitleArgs(affectedUser, affectedPoll),
                bodyArgs = getBodyArgs(affectedUser, affectedPoll),
                additionalData = mapOf(
                    "pollID" to affectedPoll?.id,
                    "userID" to affectedUser?.id
                ).toMap().filterValues { it != null }.mapValues { it.value.toString() }
            )
        }
    }

    fun sendNotification(notification: ExpollNotification, affectedPoll: Poll, affectedUser: User? = null) {
        lastNotification = Triple(notification, affectedPoll, affectedUser)
        lastNotificationTime = UnixTimestamp.now()
        AnalyticsStorage.notificationCount[notification] =
            (AnalyticsStorage.notificationCount[notification] ?: 0) + 1
        affectedPoll.users.forEach {
            sendNotification(it, lastNotification)
        }
    }

    fun sendNotification(user: User, notification: Triple<ExpollNotification, Poll?, User?>) {
        if (config.developmentMode) return
        if (notification.first.isWantedByUser(user).not()) return

        val apnDevices = user.apnDevices
        val webDevices = user.webNotificationDevices
        val universalNotification = notification.first.asUniversalNotification(
            affectedPoll = notification.second,
            affectedUser = notification.third
        )

        apnDevices.forEach {
            if (it.session == null) return@forEach
            APNsNotificationHandler.sendNotification(universalNotification, it)
        }
        webDevices.forEach {
            if (it.session == null) return@forEach
            WebNotificationHandler.sendNotification(universalNotification, it)
        }
    }
}