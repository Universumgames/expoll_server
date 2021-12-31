import { config } from "./../config"
import axios from "axios"
import { checkLoggedIn } from "./routeHelper"
import { cookieName, getLoginKey } from "./../helper"
import { ReturnCode } from "expoll-lib/interfaces"
import { Session, User } from "./../entities/entities"
import express, { CookieOptions, NextFunction, Request, Response } from "express"
import getUserManager from "../UserManagement"
import { CreateUserRequest, CreateUserResponse } from "expoll-lib/requestInterfaces"

// eslint-disable-next-line new-cap
const userRoutes = express.Router()

/* const test = async (req: Request, res: Response, next: NextFunction) => {
    return res.status(200).json({
        message: "Test successful"
    })
} */

/**
 * Verify received captcha token
 * @param {string} token the token received by the client
 * @return {boolean} true if token is valid false otherwise
 */
async function verifyCaptcha(token: string): Promise<boolean> {
    const googleReturn = (
        await axios.post("https://www.google.com/recaptcha/api/siteverify", undefined, {
            params: {
                secret: config.recaptchaAPIKey,
                response: token
            }
        })
    ).data
    console.log(googleReturn)
    return googleReturn.score >= 0.5 ?? false
}

const createUser = async (req: Request, res: Response, next: NextFunction) => {
    try {
        // check valid request body
        if (req.body == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
        const body = req.body as CreateUserRequest
        const mail = body.mail as string
        if (mail == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
        const firstName = body.firstName as string
        if (firstName == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
        const lastName = body.lastName as string
        if (lastName == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
        const username = body.username as string
        if (username == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
        if (body.captcha == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
        const captchaToken = body.captcha

        if (!(await verifyCaptcha(captchaToken))) return res.status(ReturnCode.CAPTCHA_INVALID).end()

        // check user not exist
        if (
            (await getUserManager().checkUserExists({ mail: mail })) ||
            (await getUserManager().checkUserExists({ username: username }))
        )
            return res.status(ReturnCode.USER_EXISTS).end()
        // create user
        const user = new User()
        user.mail = mail
        user.firstName = firstName
        user.lastName = lastName
        user.username = username
        try {
            await user.save()

            const session = await user.generateSession()
            const loginKey = session.loginKey

            const data: CreateUserResponse = {
                loginKey: loginKey
            }
            return res.status(ReturnCode.OK).cookie(cookieName, data, cookieConfig(session)).json(data)
        } catch (e) {
            console.error(e)
            return res.status(500).end()
        }
    } catch (e) {
        console.error(e)
        res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
    }
}

const getUserData = async (req: Request, res: Response, next: NextFunction) => {
    try {
        // @ts-ignore
        const user = req.user as User
        // @ts-ignore
        const loginKey = req.loginKey as string

        /* const loginKey = getLoginKey(req)
        if (loginKey == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
        const user = await getUserManager().getUser({ loginKey: loginKey })
        if (user == undefined)
        return res.status(ReturnCode.INVALID_LOGIN_KEY).cookie(cookieName, {}).end() // unauthorized */

        const easyUser = user
        easyUser.admin = await getUserManager().userIsAdminOrSuperAdmin(user.id)

        const data = {
            loginKey: loginKey
        }
        const session = await getUserManager().getSession(loginKey)
        return res.status(ReturnCode.OK).cookie(cookieName, data, cookieConfig(session!)).json(easyUser)
    } catch (e) {
        console.error(e)
        res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
    }
}

const login = async (req: Request, res: Response, next: NextFunction) => {
    try {
        // @ts-ignore
        const loginKey = getLoginKey(req)
        if (loginKey == undefined) {
            return res.status(await getUserManager().sendLoginMail(req.body.mail, req)).end()
        }
        const user = await getUserManager().getUser({ loginKey: loginKey })
        if (user == undefined) {
            return res.status(ReturnCode.INVALID_LOGIN_KEY).cookie(cookieName, {}).end() // unauthorized
        }
        const data = {
            loginKey: loginKey
        }
        const session = await getUserManager().getSession(loginKey)
        if (session == undefined || !session.isValid()) return res.status(ReturnCode.INVALID_LOGIN_KEY).end()
        return res.status(ReturnCode.OK).cookie(cookieName, data, cookieConfig(session!)).json(user)
    } catch (e) {
        console.error(e)
        res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
    }
}

/**
 * create cookie config with expiration date
 * @param {ISession} session the user session
 * @return {CookieConfig} necessary cookie config for express
 */
function cookieConfig(session: Session): CookieOptions {
    return { httpOnly: true, sameSite: "strict", expires: session.expiration }
}

const logout = async (req: Request, res: Response, next: NextFunction) => {
    return res.status(ReturnCode.OK).cookie(cookieName, "", { httpOnly: true, sameSite: "strict" }).end()
}

userRoutes.post("/", createUser)
userRoutes.get("/", checkLoggedIn, getUserData)
userRoutes.post("/login", login)
userRoutes.post("/logout", logout)
// userRoutes.all("/metaInfo", metaInfo)

export default userRoutes
