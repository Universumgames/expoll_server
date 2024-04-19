package net.mt32.expoll.notification

import net.mt32.expoll.VoteValue
import net.mt32.expoll.analytics.AnalyticsStorage
import net.mt32.expoll.config
import net.mt32.expoll.entities.Poll
import net.mt32.expoll.entities.User
import net.mt32.expoll.entities.notifications.NotificationDevice
import net.mt32.expoll.helper.UnixTimestamp
import net.mt32.expoll.helper.async
import net.mt32.expoll.tOptionID


object ExpollNotificationHandler {

    var lastNotification: DataHandler? = null
    var lastNotificationTime: UnixTimestamp = UnixTimestamp.zero()

    enum class ExpollNotification(
        val body: String,
        val title: String = "notification.poll.update %@",
        val bodyArgIndicesRange: List<RequiredArg> = listOf(),
        val titleArgIndicesRange: List<RequiredArg> = listOf()
    ) {
        EMPTY(""),
        STARTUP(
            "notification.server.backend.update %@",
            "notification.server.backend.update.title",
            listOf(RequiredArg.SERVER_VERSION),
            titleArgIndicesRange = listOf(RequiredArg.EMPTY)
        ), // server version
        NewLogin("notification.newSession", "notification.newSession"),
        VoteChange(
            "notification.vote.change %@ %@",
            bodyArgIndicesRange = listOf(RequiredArg.USER, RequiredArg.POLL),
            titleArgIndicesRange = listOf(RequiredArg.POLL)
        ), // username, poll name
        VoteChangeDetailed(
            "notification.vote.change.detailed %1$@ %2$@ %3$@ %4$@",
            bodyArgIndicesRange = listOf(
                RequiredArg.USER,
                RequiredArg.POLL,
                RequiredArg.VOTE_CHANGE,
                RequiredArg.OPTION,
            ),
            titleArgIndicesRange = listOf(RequiredArg.POLL)
        ), // username, poll name, option name, vote change
        UserAdded(
            "notification.user.added %@ %@",
            bodyArgIndicesRange = listOf(RequiredArg.USER, RequiredArg.POLL),
            titleArgIndicesRange = listOf(RequiredArg.POLL)
        ), // username, poll name
        UserRemoved(
            "notification.user.removed %@ %@",
            bodyArgIndicesRange = listOf(RequiredArg.USER, RequiredArg.POLL),
            titleArgIndicesRange = listOf(RequiredArg.POLL)
        ), // username, poll name
        PollDeleted(
            "notification.poll.delete %@",
            bodyArgIndicesRange = listOf(RequiredArg.POLL),
            titleArgIndicesRange = listOf(RequiredArg.POLL)
        ), // poll name
        PollEdited(
            "notification.poll.edited %@",
            bodyArgIndicesRange = listOf(RequiredArg.POLL),
            titleArgIndicesRange = listOf(RequiredArg.POLL)
        ), // poll name
        PollArchived(
            "notification.poll.archived %@",
            bodyArgIndicesRange = listOf(RequiredArg.POLL),
            titleArgIndicesRange = listOf(RequiredArg.POLL)
        ); // poll name

        enum class RequiredArg {
            EMPTY, SERVER_VERSION, USER, POLL, VOTE_CHANGE, OPTION
        }

        fun getBodyArgs(
            poll: Poll?,
            user: User?,
            optionString: String?,
            oldVote: VoteValue?,
            newVote: VoteValue?
        ): List<String> {
            val list = mutableListOf<String>()

            list.addAll(getArg(bodyArgIndicesRange, user, poll, optionString, oldVote, newVote))

            return list
        }

        fun getArg(
            arg: RequiredArg,
            user: User? = null,
            poll: Poll? = null,
            optionString: String? = null,
            oldVote: VoteValue? = null,
            newVote: VoteValue? = null
        ): List<String> {
            return when (arg) {
                RequiredArg.SERVER_VERSION -> listOf(config.serverVersion)
                RequiredArg.POLL -> listOf(poll?.name ?: "")
                RequiredArg.USER -> listOf(user?.let {
                    it.username.substring(
                        0,
                        minOf(it.username.length, 20)
                    ) + if (it.username.length > 20) "..." else ""
                } ?: "")

                RequiredArg.VOTE_CHANGE -> listOf(newVote!!.translationKey)
                RequiredArg.OPTION -> listOf(optionString ?: "")
                RequiredArg.EMPTY -> listOf()
            }
        }

        fun getArg(
            arg: List<RequiredArg>,
            user: User? = null,
            poll: Poll? = null,
            optionString: String? = null,
            oldVote: VoteValue? = null,
            newVote: VoteValue? = null
        ): List<String> {
            val list = mutableListOf<String>()
            arg.forEach {
                list.addAll(getArg(it, user, poll, optionString, oldVote, newVote))
            }
            return list
        }

        fun getTitleArgs(
            poll: Poll?,
            user: User?,
            optionString: String?,
            oldVote: VoteValue?,
            newVote: VoteValue?
        ): List<String> {
            val list = mutableListOf<String>()
            list.addAll(getArg(titleArgIndicesRange, user, poll, optionString, oldVote, newVote))
            return list
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
                VoteChangeDetailed -> notificationPreferences.voteChangeDetailed
                NewLogin -> notificationPreferences.newLogin
            }
        }

        fun asUniversalNotification(
            poll: Poll?,
            user: User?,
            optionID: tOptionID?,
            oldVote: VoteValue?,
            newVote: VoteValue?
        ): UniversalNotification {
            val optionString = optionID?.let { poll?.options?.find { it.id == optionID }?.toString() }
            return UniversalNotification(
                title,
                body,
                titleArgs = getTitleArgs(poll, user, optionString, oldVote, newVote),
                bodyArgs = getBodyArgs(poll, user, optionString, oldVote, newVote),
                additionalData = mapOf(
                    "pollID" to poll?.id,
                    "userID" to poll?.id,
                    "optionID" to optionID
                ).toMap().filterValues { it != null }.mapValues { it.value.toString() }
            )
        }
    }

    fun sendNotification(dataHandler: DataHandler) {
        async {
            lastNotification = dataHandler
            lastNotificationTime = UnixTimestamp.now()
            AnalyticsStorage.notificationCount[dataHandler.notification] =
                (AnalyticsStorage.notificationCount[dataHandler.notification] ?: 0) + 1
            var altNotification: DataHandler? = null
            val affectedUsers =
                if (dataHandler.poll?.privateVoting == true) listOf(
                    dataHandler.poll.admin,
                    dataHandler.user
                ) else dataHandler.poll?.users
            affectedUsers?.forEach {
                val user = it ?: return@forEach
                if (config.developmentMode && !user.superAdminOrAdmin) {
                    println("Notification suppressed in development mode for ${user.username}")
                    return@forEach
                }
                if (dataHandler.notification == ExpollNotification.VoteChange) {
                    if (altNotification == null)
                        altNotification = DataHandler(
                            ExpollNotification.VoteChangeDetailed,
                            dataHandler.poll,
                            dataHandler.user,
                            dataHandler.optionID,
                            dataHandler.oldVote,
                            dataHandler.newVote
                        )
                    if (ExpollNotification.VoteChangeDetailed.isWantedByUser(user)) {
                        sendNotification(user, altNotification!!)
                        return@forEach
                    }
                }
                sendNotification(user, lastNotification!!)
            }
        }
    }

    fun sendServerStartup() {
        val admins = User.admins()
        admins.forEach {
            sendNotification(it, DataHandler(ExpollNotification.STARTUP, null, null, null, null, null))
        }
    }

    fun sendVoteChange(poll: Poll, user: User, optionID: tOptionID, oldVote: VoteValue?, newVote: VoteValue) {
        sendNotification(DataHandler(ExpollNotification.VoteChange, poll, user, optionID, oldVote, newVote))
    }

    fun sendPollLeave(poll: Poll, user: User) {
        sendNotification(DataHandler(ExpollNotification.UserRemoved, poll, user, null, null, null))
    }

    fun sendPollJoin(poll: Poll, user: User) {
        sendNotification(DataHandler(ExpollNotification.UserAdded, poll, user, null, null, null))
    }

    fun sendPollEdit(poll: Poll) {
        sendNotification(DataHandler(ExpollNotification.PollEdited, poll, null, null, null, null))
    }

    fun sendPollDelete(poll: Poll) {
        sendNotification(DataHandler(ExpollNotification.PollDeleted, poll, null, null, null, null))
    }

    fun sendNewLogin(user: User) {
        sendNotification(DataHandler(ExpollNotification.NewLogin, null, user, null, null, null))
    }

    data class DataHandler(
        val notification: ExpollNotification,
        val poll: Poll?,
        val user: User?,
        val optionID: tOptionID?,
        val oldVote: VoteValue?,
        val newVote: VoteValue?
    )

    fun sendNotification(
        user: User,
        notification: DataHandler
    ) {
        //if (config.developmentMode) return
        if (notification.notification.isWantedByUser(user).not()) return

        val universalNotification = notification.notification.asUniversalNotification(
            poll = notification.poll,
            user = notification.user,
            optionID = notification.optionID,
            oldVote = notification.oldVote,
            newVote = notification.newVote
        )

        sendNotificationInternal(user, universalNotification)
    }

    private fun sendNotificationInternal(user: User, universalNotification: UniversalNotification) {
        val notificationDevices = user.notificationDevices
        notificationDevices.forEach {
            if (it.isValid)
                it.sendNotification(universalNotification)
        }
    }

    // TODO improve error handling
    fun sendInternalErrorNotification(error: String) {
        val admins = User.admins()
        admins.forEach {
            sendNotificationInternal(
                it, UniversalNotification(
                    "An Internal Server Error occurred",
                    body = error
                )
            )
        }
    }

    object Example {
        /**
         * Notifications:  Type (body args) (title args)
         *                 VoteChange (User, Poll) (Poll)
         *                 UserAdded (User, Poll) (Poll)
         *                 UserRemoved  (User, Poll) (Poll)
         *                 PollDeleted (Poll) (Poll)
         *                 PollEdited (Poll) (Poll)
         *                 PollArchived (Poll) (Poll)
         *                 VoteChangeDetailed (User, Poll, VoteChange, Option) (Poll)
         *                 NewLogin () ()
         */

        private val voteChange = UniversalNotification(
            ExpollNotification.VoteChange.title,
            ExpollNotification.VoteChange.body,
            titleArgs = listOf("Example Poll"),
            bodyArgs = listOf("Example User", "Example Poll"),
        )

        private val userAdded = UniversalNotification(
            ExpollNotification.UserAdded.title,
            ExpollNotification.UserAdded.body,
            titleArgs = listOf("Example Poll"),
            bodyArgs = listOf("Example User", "Example Poll"),
        )

        private val userRemoved = UniversalNotification(
            ExpollNotification.UserRemoved.title,
            ExpollNotification.UserRemoved.body,
            titleArgs = listOf("Example Poll"),
            bodyArgs = listOf("Example User", "Example Poll"),
        )

        private val pollDeleted = UniversalNotification(
            ExpollNotification.PollDeleted.title,
            ExpollNotification.PollDeleted.body,
            titleArgs = listOf("Example Poll"),
            bodyArgs = listOf("Example Poll"),
        )

        private val pollEdited = UniversalNotification(
            ExpollNotification.PollEdited.title,
            ExpollNotification.PollEdited.body,
            titleArgs = listOf("Example Poll"),
            bodyArgs = listOf("Example Poll"),
        )

        private val pollArchived = UniversalNotification(
            ExpollNotification.PollArchived.title,
            ExpollNotification.PollArchived.body,
            titleArgs = listOf("Example Poll"),
            bodyArgs = listOf("Example Poll"),
        )

        private val voteChangeDetailed = UniversalNotification(
            ExpollNotification.VoteChangeDetailed.title,
            ExpollNotification.VoteChangeDetailed.body,
            titleArgs = listOf("Example Poll"),
            bodyArgs = listOf("Example User", "Example Poll", "Example VoteChange", "Example Option"),
        )

        private val newLogin = UniversalNotification(
            ExpollNotification.NewLogin.title,
            ExpollNotification.NewLogin.body,
            titleArgs = listOf(),
            bodyArgs = listOf(),
        )


        fun sendExampleVoteChange(notificationDevice: NotificationDevice) {
            notificationDevice.sendNotification(voteChange)
        }

        fun sendExampleUserAdded(notificationDevice: NotificationDevice) {
            notificationDevice.sendNotification(userAdded)
        }

        fun sendExampleUserRemoved(notificationDevice: NotificationDevice) {
            notificationDevice.sendNotification(userRemoved)
        }

        fun sendExamplePollDeleted(notificationDevice: NotificationDevice) {
            notificationDevice.sendNotification(pollDeleted)
        }

        fun sendExamplePollEdited(notificationDevice: NotificationDevice) {
            notificationDevice.sendNotification(pollEdited)
        }

        fun sendExamplePollArchived(notificationDevice: NotificationDevice) {
            notificationDevice.sendNotification(pollArchived)
        }

        fun sendExampleVoteChangeDetailed(notificationDevice: NotificationDevice) {
            notificationDevice.sendNotification(voteChangeDetailed)
        }

        fun sendExampleNewLogin(notificationDevice: NotificationDevice) {
            notificationDevice.sendNotification(newLogin)
        }
    }
}