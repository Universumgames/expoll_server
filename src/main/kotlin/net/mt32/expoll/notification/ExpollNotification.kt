package net.mt32.expoll.notification

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.mt32.expoll.entities.Poll
import net.mt32.expoll.entities.User
import net.mt32.expoll.helper.UnixTimestamp
import net.mt32.expoll.tPollID
import net.mt32.expoll.tUserID

enum class ExpollNotificationType(val body: String) {
    VoteChange("notification.vote.change %@ %@"),
    UserAdded("notification.user.added %@ %@"),
    UserRemoved("notification.user.removed %@ %@"),
    PollDeleted("notification.poll.delete %@"),
    PollEdited("notification.poll.edited %@"),
    PollArchived("notification.poll.archived %@");

    fun notificationArgs(poll: Poll, user: User?): List<String> {
        val pollUpdate =
            this == PollArchived ||
                    this == PollDeleted ||
                    this == PollEdited ||
                    this == UserAdded ||
                    this == UserRemoved ||
                    this == VoteChange
        val userUpdate =
            this == UserAdded ||
                    this == UserRemoved ||
                    this == VoteChange
        val pollString = if (pollUpdate) poll.name else null
        val userString = if (userUpdate) (user?.firstName + " " + user?.lastName) else null
        return listOf(userString, pollString).filterNotNull()
    }
}

data class ExpollNotification(
    val type: ExpollNotificationType,
    val pollID: tPollID,
    val affectedUserID: tUserID?
)

fun userWantNotificationType(type: ExpollNotificationType, user: User): Boolean {
    val notificationPreferences = user.notificationPreferences
    return when (type) {
        ExpollNotificationType.VoteChange -> notificationPreferences.voteChange
        ExpollNotificationType.UserAdded -> notificationPreferences.userAdded
        ExpollNotificationType.UserRemoved -> notificationPreferences.userRemoved
        ExpollNotificationType.PollDeleted -> notificationPreferences.pollDeleted
        ExpollNotificationType.PollEdited -> notificationPreferences.pollEdited
        ExpollNotificationType.PollArchived -> notificationPreferences.pollArchived
    }
}

// TODO implement sending notification async on event
@OptIn(DelicateCoroutinesApi::class)
fun sendNotification(notification: ExpollNotification) {
    GlobalScope.launch {
        val poll = Poll.fromID(notification.pollID)
        val affectedUser = notification.affectedUserID?.let { User.loadFromID(it) }
        if (poll == null) return@launch
        val apnsNotification = APNsNotification(
            "Poll ${poll.name} was updated",
            null,
            null,
            bodyLocalisationKey = notification.type.body,
            bodyLocalisationArgs = notification.type.notificationArgs(poll, affectedUser)
        )
        val payload = APNsPayload(APS(apnsNotification))
        val expiration = UnixTimestamp.now().addDays(5)
        poll.users.forEach { user ->
            if (!userWantNotificationType(notification.type, user)) return@forEach

            user.apnDevices.forEach { device ->
                runBlocking {
                    APNsNotificationHandler.sendAPN(device.deviceID, expiration, payload, APNsPriority.medium)
                }
            }
        }
    }
}