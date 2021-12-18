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
    const errorReturn = res.status(400)
    if (req.body == undefined) return errorReturn
    const body = req.body
    const mail = body.mail
    if (mail == undefined) return errorReturn
    const firstName = body.firstName
    if (firstName == undefined) return errorReturn
    const lastName = body.lastName
    if (lastName == undefined) return errorReturn
    const username = body.username
    if (username == undefined) return errorReturn
    // check user not exist
    if (await getUserManager().checkUserExists(mail)) return res.status(406)
    // create user
    const user = new User()
    user.mail = mail
    user.firstName = firstName
    user.lastName = lastName
    user.username = username
    const loginKey = user.loginKey
    user.save()

    const data = {
        loginKey: loginKey
    }
    return res.status(200).cookie("expoll_user_data", data, { httpOnly: true }).json(data)
}

userRoutes.post("/", createUser)
// userRoutes.all("/metaInfo", metaInfo)

export default userRoutes
