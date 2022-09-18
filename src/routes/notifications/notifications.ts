import { NotificationPreferencesEntity } from "../../entities/notifications"
import { User } from "../../entities/user"
import { NotificationPreferences, ReturnCode } from "expoll-lib"
import express, { NextFunction, Request, Response } from "express"
import { checkLoggedIn } from "../routeHelper"
import appleNotificationRoutes from "./apple"

// eslint-disable-next-line new-cap
const notificationRoutes = express.Router()

const test = async (req: Request, res: Response, next: NextFunction) => {
    return res.status(200).json({
        message: "Test successful"
    })
}

const getPreferences = async (req: Request, res: Response, next: NextFunction) => {
    try {
        // @ts-ignore
        const user = req.user as User
        let preferences = await NotificationPreferencesEntity.findOne({ where: { user: user } })
        if (preferences == undefined) {
            preferences = new NotificationPreferencesEntity()
            preferences.user = user

            await preferences.save()
        }
        const strippedPreferences: any = Object.assign({}, preferences)
        strippedPreferences.user = undefined
        strippedPreferences.id = undefined
        return res.status(ReturnCode.OK).json(strippedPreferences as NotificationPreferences)
    } catch (e) {
        console.error(e)
        return res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
    }
}

const setPreferences = async (req: Request, res: Response, next: NextFunction) => {
    try {
        // @ts-ignore
        const user = req.user as User
        const simplePreferences = req.body as NotificationPreferences
        let preferences = await NotificationPreferencesEntity.findOne({ where: { user: user } })
        if (preferences == undefined) {
            preferences = new NotificationPreferencesEntity()
            preferences.user = user
        }

        preferences.pollArchived = simplePreferences.pollArchived
        preferences.pollDeleted = simplePreferences.pollDeleted
        preferences.pollEdited = simplePreferences.pollEdited
        preferences.userAdded = simplePreferences.userAdded
        preferences.userRemoved = simplePreferences.userRemoved
        preferences.voteChange = simplePreferences.voteChange

        await preferences.save()

        const strippedPreferences: any = Object.assign({}, preferences)
        strippedPreferences.user = undefined
        strippedPreferences.id = undefined
        return res.status(ReturnCode.OK).json(strippedPreferences as NotificationPreferences)
    } catch (e) {
        console.error(e)
        return res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
    }
}

notificationRoutes.use("/apple", appleNotificationRoutes)
notificationRoutes.all("/", test)
notificationRoutes.get("/preferences", checkLoggedIn, getPreferences)
notificationRoutes.post("/preferences", checkLoggedIn, setPreferences)

export default notificationRoutes
