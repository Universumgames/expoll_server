package net.mt32.expoll.notification

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.mt32.expoll.analytics.AnalyticsStorage
import net.mt32.expoll.config
import net.mt32.expoll.entities.APNDevice
import net.mt32.expoll.entities.Poll
import net.mt32.expoll.entities.User
import net.mt32.expoll.helper.UnixTimestamp
import net.mt32.expoll.tPollID
import net.mt32.expoll.tUserID

interface IExpollNotificationType {
    val body: String
    val title: String
}

enum class ExpollNotificationType(override val body: String, override val title: String="\$poll") : IExpollNotificationType {
    EMPTY(""),
    STARTUP("notification.server.backend.update %@"),
    VoteChange("notification.vote.change %@ %@"),
    UserAdded("notification.user.added %@ %@"),
    UserRemoved("notification.user.removed %@ %@"),
    PollDeleted("notification.poll.delete %@"),
    PollEdited("notification.poll.edited %@"),
    PollArchived("notification.poll.archived %@");

    fun notificationArgs(poll: Poll, user: User?): List<String> {
        if(this == STARTUP) return listOf(config.serverVersion)
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

interface IExpollNotification {
    val type: ExpollNotificationType
}

data class ExpollNotification(
    override val type: ExpollNotificationType,
    val pollID: tPollID,
    val affectedUserID: tUserID?
) : IExpollNotification {
    override fun equals(other: Any?): Boolean {
        if (other !is ExpollNotification) return false
        return type == other.type && pollID == other.pollID && affectedUserID == other.affectedUserID
    }
}

fun userWantNotificationType(type: ExpollNotificationType, user: User): Boolean {
    val notificationPreferences = user.notificationPreferences
    return when (type) {
        ExpollNotificationType.EMPTY -> false
        ExpollNotificationType.STARTUP -> user.admin
        ExpollNotificationType.VoteChange -> notificationPreferences.voteChange
        ExpollNotificationType.UserAdded -> notificationPreferences.userAdded
        ExpollNotificationType.UserRemoved -> notificationPreferences.userRemoved
        ExpollNotificationType.PollDeleted -> notificationPreferences.pollDeleted
        ExpollNotificationType.PollEdited -> notificationPreferences.pollEdited
        ExpollNotificationType.PollArchived -> notificationPreferences.pollArchived
    }
}

@Serializable
@SerialName("expollPayload")
class ExpollAPNsPayload(
    override val aps: APS,
    val pollID: tPollID? = null
) : IAPNsPayload

var lastNotification: ExpollNotification = ExpollNotification(ExpollNotificationType.EMPTY, "", null)
var lastNotificationTime: UnixTimestamp = UnixTimestamp.zero()

fun sendNotificationAllowed(notification: ExpollNotification): Boolean {
    if (config.developmentMode) return false
    if (lastNotification == notification && lastNotificationTime.addMinutes(1) > UnixTimestamp.now()) {
        return false
    }
    lastNotification = notification
    lastNotificationTime = UnixTimestamp.now()
    return true
}

@OptIn(DelicateCoroutinesApi::class)
fun sendNotification(notification: ExpollNotification) {
    //if (!sendNotificationAllowed(notification)) return
    AnalyticsStorage.notificationCount[notification.type] =
        (AnalyticsStorage.notificationCount[notification.type] ?: 0) + 1
    GlobalScope.launch {
        val poll = Poll.fromID(notification.pollID)
        val affectedUser = notification.affectedUserID?.let { User.loadFromID(it) }
        if (poll == null) return@launch
        val apnsNotification = APNsNotification(
            notification.type.title.replace("\$poll", "Poll ${poll.name} was updated"),
            null,
            null,
            bodyLocalisationKey = notification.type.body,
            bodyLocalisationArgs = notification.type.notificationArgs(poll, affectedUser)
        )
        val payload = ExpollAPNsPayload(APS(apnsNotification), poll.id)
        val expiration = UnixTimestamp.now().addDays(5)
        poll.users.forEach { user ->
            if (!userWantNotificationType(notification.type, user)) return@forEach

            sendNotification(payload, user, expiration, APNsPriority.medium)
        }
    }
}

fun sendNotification(payload: IAPNsPayload, user: User, expiration: UnixTimestamp, priority: APNsPriority) {
    sendNotification(payload, user.apnDevices, expiration, priority)
}

fun sendNotification(
    payload: IAPNsPayload,
    devices: List<APNDevice>,
    expiration: UnixTimestamp,
    priority: APNsPriority
) {
    devices.forEach { device ->
        sendNotification(payload, device, expiration, priority)
    }
}

fun sendNotification(payload: IAPNsPayload, device: APNDevice, expiration: UnixTimestamp, priority: APNsPriority) {
    if (device.session == null)
        device.delete()
    else
        runBlocking {
            APNsNotificationHandler.sendAPN(device.deviceID, expiration, payload, priority)
        }
}