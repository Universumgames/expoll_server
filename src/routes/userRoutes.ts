import { cookieName } from "./../helper"
import { ReturnCode } from "./../interfaces"
import { User } from "./../entities/entities"
import express, { NextFunction, Request, Response } from "express"
import getUserManager from "../UserManagement"
import { getLoginKey } from "../helper"

// eslint-disable-next-line new-cap
const userRoutes = express.Router()

/* const test = async (req: Request, res: Response, next: NextFunction) => {
    return res.status(200).json({
        message: "Test successful"
    })
} */

const createUser = async (req: Request, res: Response, next: NextFunction) => {
    try {
        // check valid request body
        if (req.body == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
        const body = req.body
        const mail = body.mail as string
        if (mail == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
        const firstName = body.firstName as string
        if (firstName == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
        const lastName = body.lastName as string
        if (lastName == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
        const username = body.username as string
        if (username == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
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
        return res.status(ReturnCode.OK).cookie(cookieName, data, { httpOnly: true, sameSite: "strict" }).json(data)
    } catch (e) {
        console.error(e)
        res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
    }
}

const getUserData = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const loginKey = getLoginKey(req)
        const user = await getUserManager().getUser({ loginKey: loginKey })
        if (user == undefined) return res.status(ReturnCode.INVALID_LOGIN_KEY).end() // unauthorized
        const data = {
            loginKey: loginKey
        }
        return res.status(ReturnCode.OK).cookie(cookieName, data, { httpOnly: true, sameSite: "strict" }).json(user)
    } catch (e) {
        console.error(e)
        res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
    }
}

const login = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const loginKey = getLoginKey(req)
        if (loginKey == undefined) {
            return res.status(await getUserManager().sendLoginMail(req.body.mail, req)).end()
        }
        const user = await getUserManager().getUser({ loginKey: loginKey })
        if (user == undefined) return res.status(ReturnCode.INVALID_LOGIN_KEY).end() // unauthorized
        const data = {
            loginKey: loginKey
        }
        return res.status(ReturnCode.OK).cookie(cookieName, data, { httpOnly: true, sameSite: "strict" }).json(user)
    } catch (e) {
        console.error(e)
        res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
    }
}

const logout = async (req: Request, res: Response, next: NextFunction) => {
    return res.status(ReturnCode.OK).cookie(cookieName, "", { httpOnly: true, sameSite: "strict" }).end()
}

userRoutes.post("/", createUser)
userRoutes.get("/", getUserData)
userRoutes.post("/login", login)
userRoutes.post("/logout", logout)
// userRoutes.all("/metaInfo", metaInfo)

export default userRoutes
