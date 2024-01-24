package net.mt32.expoll.notification

import net.mt32.expoll.VoteValue
import net.mt32.expoll.analytics.AnalyticsStorage
import net.mt32.expoll.config
import net.mt32.expoll.entities.Poll
import net.mt32.expoll.entities.User
import net.mt32.expoll.helper.UnixTimestamp
import net.mt32.expoll.tOptionID


object ExpollNotificationHandler {

    var lastNotification: DataHandler? = null
    var lastNotificationTime: UnixTimestamp = UnixTimestamp.zero()

    enum class ExpollNotification(
        val body: String,
        val title: String = "notification.poll.update %@",
        val bodyArgIndicesRange: IntRange = 2..2,
        val titleArgIndicesRange: IntRange = 2..2
    ) {
        EMPTY(""),
        STARTUP(
            "notification.server.backend.update %@",
            "notification.server.backend.update.title",
            1..1,
            titleArgIndicesRange = 0..0
        ), // server version
        VoteChange("notification.vote.change %@ %@", bodyArgIndicesRange = 2..3), // username, poll name
        VoteChangeDetailed(
            "notification.vote.change.detailed %1$@ %2$@ %3$@ %4$@",
            bodyArgIndicesRange = 2..5
        ), // username, poll name, option name, vote change
        UserAdded("notification.user.added %@ %@", bodyArgIndicesRange = 2..3), // username, poll name
        UserRemoved("notification.user.removed %@ %@", bodyArgIndicesRange = 2..3), // username, poll name
        PollDeleted("notification.poll.delete %@", bodyArgIndicesRange = 3..3), // poll name
        PollEdited("notification.poll.edited %@", bodyArgIndicesRange = 3..3), // poll name
        PollArchived("notification.poll.archived %@", bodyArgIndicesRange = 3..3); // poll name

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
            for (i in bodyArgIndicesRange) {
                val arg = RequiredArg.values()[i]
                list.addAll(getArg(arg, user, poll, optionString, oldVote, newVote))
            }
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

        fun getTitleArgs(
            poll: Poll?,
            user: User?,
            optionString: String?,
            oldVote: VoteValue?,
            newVote: VoteValue?
        ): List<String> {
            val list = mutableListOf<String>()
            for (i in titleArgIndicesRange) {
                val arg = RequiredArg.values()[i]
                list.addAll(getArg(arg, user, poll, optionString, oldVote, newVote))
            }
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
        lastNotification = dataHandler
        lastNotificationTime = UnixTimestamp.now()
        AnalyticsStorage.notificationCount[dataHandler.notification] =
            (AnalyticsStorage.notificationCount[dataHandler.notification] ?: 0) + 1
        var altNotification: DataHandler? = null
        val affectedUsers = if(dataHandler.poll?.privateVoting == true) listOf(dataHandler.poll.admin) else dataHandler.poll?.users
        affectedUsers?.forEach {
            if (dataHandler.notification == ExpollNotification.VoteChange) {
                if(altNotification == null)
                    altNotification = DataHandler(
                        ExpollNotification.VoteChangeDetailed,
                        dataHandler.poll,
                        dataHandler.user,
                        dataHandler.optionID,
                        dataHandler.oldVote,
                        dataHandler.newVote
                    )
                if (ExpollNotification.VoteChangeDetailed.isWantedByUser(it)) {
                    sendNotification(it, altNotification!!)
                    return@forEach
                }
            }
            sendNotification(it, lastNotification!!)
        }
    }

    fun sendServerStartup(){
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

    private fun sendNotificationInternal(user: User, universalNotification: UniversalNotification){
        val apnDevices = user.apnDevices
        val webDevices = user.webNotificationDevices

        apnDevices.forEach {
            if (it.session == null) return@forEach
            APNsNotificationHandler.sendNotification(universalNotification, it)
        }
        webDevices.forEach {
            if (it.session == null) return@forEach
            WebNotificationHandler.sendNotification(universalNotification, it)
        }
    }

    // TODO improve error handling
    fun sendInternalErrorNotification(error: String){
        val admins = User.admins()
        admins.forEach {
            sendNotificationInternal(it, UniversalNotification(
                "An Internal Server Error occured",
                body = error
            ))
        }
    }
}