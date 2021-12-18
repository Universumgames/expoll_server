import { User } from "./../entities/entities"
import express, { NextFunction, Request, Response } from "express"
import getUserManager from "../UserManagement"

// eslint-disable-next-line new-cap
const userRoutes = express.Router()

/* const test = async (req: Request, res: Response, next: NextFunction) => {
    return res.status(200).json({
        message: "Test successful"
    })
} */

const createUser = async (req: Request, res: Response, next: NextFunction) => {
    // check valid request body
    const missingFieldRC = 400
    if (req.body == undefined) return res.status(missingFieldRC).end()
    const body = req.body
    const mail = body.mail as string
    if (mail == undefined) return res.status(missingFieldRC).end()
    const firstName = body.firstName as string
    if (firstName == undefined) return res.status(missingFieldRC).end()
    const lastName = body.lastName as string
    if (lastName == undefined) return res.status(missingFieldRC).end()
    const username = body.username as string
    if (username == undefined) return res.status(missingFieldRC).end()
    // check user not exist
    if (await getUserManager().checkUserExists({ mail: mail })) return res.status(406).end()
    // create user
    const user = new User()
    user.mail = mail
    user.firstName = firstName
    user.lastName = lastName
    user.username = username
    const loginKey = user.loginKey
    try {
        await user.save()
    } catch (e) {
        console.error(e)
        return res.status(500).end()
    }

    const data = {
        loginKey: loginKey
    }
    return res.status(200).cookie("expoll_user_data", data, { httpOnly: true }).json(data)
}

userRoutes.post("/", createUser)
// userRoutes.all("/metaInfo", metaInfo)

export default userRoutes
