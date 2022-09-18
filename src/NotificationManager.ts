import { AxiosResponse } from "axios"
import { APNsDevice } from "./entities/apnDevice"
import { NotificationPreferencesEntity } from "./entities/notifications"
import { Poll } from "./entities/poll"
import { APNsNotification, APNsPayload, APNsPriority, APNsPushType, sendAPN } from "./notificationHelper"
import { NotificationType } from "expoll-lib"
import { User } from "./entities/user"
import { Vote } from "./entities/vote"

/**
 * Manager class to send notifications
 */
class NotificationManager {
    /**
     * Create new notification object
     */
    constructor() {}

    /**
     * send notifications on poll update to all participants
     * @param {User} sender the user that updated the poll
     * @param {Poll} poll the poll that was updated
     * @param {NotificationType} notificationType the type of the poll update
     * @param {any} data additional data for the poll update
     * @return {Promise<void>}
     */
    onPollUpdate(sender: User, poll: Poll, notificationType: NotificationType, data: { user?: User }): Promise<void> {
        return new Promise(async (resolve, reject) => {
            try {
                const participantsUnfiltered = (
                    await Vote.find({
                        where: { poll: poll },
                        relations: ["user"]
                    })
                ).map((vote) => {
                    return vote.user
                })

                const participants: User[] = []
                participantsUnfiltered.forEach((user) => {
                    if (!participants.find((u) => u.id == user.id)) {
                        participants.push(user)
                    }
                })

                for (const participant of participants) {
                    // skip the user that updated the poll
                    // if (sender.id == participant.id) continue

                    // check if the user has the notification enabled
                    const devices = await APNsDevice.find({ where: { user: participant }, relations: ["user"] })

                    let notificationPreferences = await NotificationPreferencesEntity.findOne({
                        where: { user: participant }
                    })

                    if (!notificationPreferences) {
                        notificationPreferences = new NotificationPreferencesEntity()
                        notificationPreferences.user = participant
                        notificationPreferences = await notificationPreferences.save()
                    }

                    if (!this.notificationAllowed(notificationPreferences, notificationType)) continue

                    // send the notification to all devices
                    const notification: APNsNotification = {
                        "loc-key": this.getNotificationBody(notificationType),
                        "loc-args": this.notificationTitleArgs(notificationType, poll, data.user),
                        title: `Poll ${poll.name} was updated`
                    }

                    for (const device of devices) {
                        this.sendNotification(device.deviceID, notification, poll.id)
                    }
                }
                resolve()
            } catch (e) {
                console.warn(e)
                reject(e)
            }
        })
    }

    /**
     * Check if a user has the required notifications enabled
     * @param {NotificationPreferencesEntity} notificationPreferences the users notification preferences
     * @param {NotificationType}notificationType the type of the notification to check
     * @return {boolean} true if the user has the notification enabled
     */
    private notificationAllowed(
        notificationPreferences: NotificationPreferencesEntity,
        notificationType: NotificationType
    ): boolean {
        switch (notificationType) {
            case NotificationType.pollArchived:
                return notificationPreferences.pollArchived
            case NotificationType.pollEdited:
                return notificationPreferences.pollEdited
            case NotificationType.pollDeleted:
                return notificationPreferences.pollDeleted
            case NotificationType.userAdded:
                return notificationPreferences.userAdded
            case NotificationType.userRemoved:
                return notificationPreferences.userRemoved
            case NotificationType.voteChange:
                return notificationPreferences.voteChange
        }
    }

    /**
     * notification arguments for a poll
     * @param {NotificationType} notificationType  the type of the notification
     * @param {Poll} poll  the poll that was updated
     * @param {User} user the user that was added or removed
     * @return {String[]} the arguments for the notification
     */
    private notificationTitleArgs(notificationType: NotificationType, poll?: Poll, user?: User): string[] {
        const pollUpdate =
            notificationType == NotificationType.pollArchived ||
            notificationType == NotificationType.pollDeleted ||
            notificationType == NotificationType.pollEdited ||
            notificationType == NotificationType.userAdded ||
            notificationType == NotificationType.userRemoved ||
            notificationType == NotificationType.voteChange
        const userUpdate =
            notificationType == NotificationType.userAdded ||
            notificationType == NotificationType.userRemoved ||
            notificationType == NotificationType.voteChange
        return [...(userUpdate ? [user!.firstName + " " + user!.lastName] : []), ...(pollUpdate ? [poll!.name] : [])]
    }

    /**
     * Title of the notification
     * @param {NotificationType} notificationType the type of the notification
     * @return {string} the title of the notification
     */
    private getNotificationBody(notificationType: NotificationType): string {
        switch (notificationType) {
            case NotificationType.pollDeleted:
                return "notification.poll.delete %@"
            case NotificationType.pollEdited:
                return "notification.poll.edited %@"
            case NotificationType.pollArchived:
                return "notification.poll.archived %@"
            case NotificationType.userAdded:
                return "notification.user.added %@ %@"
            case NotificationType.userRemoved:
                return "notification.user.removed %@ %@"
            case NotificationType.voteChange:
                return "notification.vote.change %@ %@"
        }
    }

    /**
     * send a notification to a device
     * @param {String} deviceToken the device the notification should be send to
     * @param {APNsNotification} notification the notification data
     * @param {String} pollID the id of the poll to group notifications
     */
    async sendNotification(
        deviceToken: string,
        notification: APNsNotification,
        pollID: string
    ): Promise<AxiosResponse | undefined> {
        const expiration = new Date()
        expiration.setTime(new Date().getTime() + 1000 * 60 * 60 * 24 * 5) // 5 days

        const payload: APNsPayload = {
            aps: {
                alert: notification
            },
            pollID: pollID
        }

        return sendAPN(
            deviceToken,
            expiration,
            payload,
            APNsPriority.medium,
            APNsPushType.alert,
            pollID.substring(0, 8)
        )
    }
}

let notificationManager!: NotificationManager

/**
 * creates if not already present a new NotificationManager, if existent returns existing one
 * @return {NotificationManager} User Manager to manage all User todos
 */
export function createNotificationManager(): NotificationManager {
    if (notificationManager == undefined) notificationManager = new NotificationManager()
    return notificationManager
}

/**
 * returns initialized user manager
 * @return {MailManager} current user manager
 */
export default function getNotificationManager(): NotificationManager {
    return notificationManager
}
