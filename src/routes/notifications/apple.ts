import { checkLoggedIn } from "../routeHelper"
import { User } from "./../../entities/entities"
import { ReturnCode } from "expoll-lib/interfaces"
import express, { NextFunction, Request, Response } from "express"
import { APNsDevice } from "../../entities/apnDevice"

// eslint-disable-next-line new-cap
const appleNotificationRoutes = express.Router()

/* const test = async (req: Request, res: Response, next: NextFunction) => {
    return res.status(200).json({
        message: "Test successful"
    })
} */

const register = async (req: Request, res: Response, next: NextFunction) => {
    try {
        // @ts-ignore
        const user = req.user as User
        const body = req.body
        const deviceID = body.deviceID as string

        if (deviceID == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()

        if ((await APNsDevice.findOne({ deviceID: deviceID })) != undefined)
            return res.status(ReturnCode.CHANGE_NOT_ALLOWED).end()

        const device = new APNsDevice()
        device.deviceID = deviceID
        device.user = user

        await device.save()

        return res.status(ReturnCode.OK).end()
    } catch (e) {
        console.error(e)
        return res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
    }
}

appleNotificationRoutes.post("/", checkLoggedIn, register)

export default appleNotificationRoutes
