import getMailManager, { Mail } from "./../MailManager"
import { config } from "../expoll_config"
import axios from "axios"
import { checkLoggedIn } from "./routeHelper"
import { addServerTimingsMetrics, cookieConfig, cookieName, getLoginKey } from "./../helper"
import { ReturnCode } from "expoll-lib/interfaces"
import { User } from "./../entities/entities"
import express, { NextFunction, Request, Response } from "express"
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
    return googleReturn.score >= 0.5 ?? false
}

const createUser = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const t1 = new Date()
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

        const t2 = new Date()

        if (!(await verifyCaptcha(captchaToken))) return res.status(ReturnCode.CAPTCHA_INVALID).end()

        const t3 = new Date()

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
            const t4 = new Date()
            await user.save()

            const t5 = new Date()
            const session = await user.generateSession()
            const loginKey = session.loginKey

            const data: CreateUserResponse = {
                loginKey: loginKey
            }

            const port = req.app.settings.port || config.frontEndPort
            getMailManager().sendMail({
                from: config.mailUser,
                to: user.mail,
                subject: "Thank you for registering in expoll",
                text:
                    "Thank you for creating an account at over at expoll (" +
                    req.protocol +
                    "://" +
                    config.loginLinkURL +
                    (port == 80 || port == 443 ? "" : ":" + port) +
                    ")"
            } as Mail)

            const t6 = new Date()

            // @ts-ignore
            let metrics = req.metrics
            metrics = addServerTimingsMetrics(
                metrics,
                "paramsCheck",
                "Check Required Parameters for user creation",
                t2.getTime() - t1.getTime()
            )
            metrics = addServerTimingsMetrics(metrics, "captcha", "Verify captcha", t3.getTime() - t2.getTime())
            metrics = addServerTimingsMetrics(
                metrics,
                "checkExisting",
                "Check if Mail and username is unique",
                t4.getTime() - t3.getTime()
            )
            metrics = addServerTimingsMetrics(metrics, "saveUser", "Create User in DB", t5.getTime() - t4.getTime())
            metrics = addServerTimingsMetrics(metrics, "mailSend", "Send mail to user", t6.getTime() - t5.getTime())

            return (
                res
                    // @ts-ignore
                    .set("Server-Timing", metrics)
                    .status(ReturnCode.OK)
                    .cookie(cookieName, data, cookieConfig(session))
                    .json(data)
            )
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

        const t1 = new Date()
        const easyUser = user
        const cookieSessionReq = getUserManager().getSession(loginKey)

        easyUser.admin = getUserManager().userIsAdminOrSuperAdminSync(user)

        const data = {
            loginKey: loginKey
        }
        const session = await cookieSessionReq
        const t2 = new Date()

        return (
            res
                // @ts-ignore
                .set(
                    "Server-Timing",
                    addServerTimingsMetrics(
                        // @ts-ignore
                        req.metrics,
                        "cookieSet",
                        "Transform userdata and set cookie",
                        t2.getTime() - t1.getTime()
                    )
                )
                .status(ReturnCode.OK)
                .cookie(cookieName, data, cookieConfig(session!))
                .json(easyUser)
        )
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

const logout = async (req: Request, res: Response, next: NextFunction) => {
    return res.status(ReturnCode.OK).cookie(cookieName, "", { httpOnly: true, sameSite: "strict" }).end()
}

const deleteUser = async (req: Request, res: Response, next: NextFunction) => {
    try {
        // @ts-ignore
        const user = req.user as User

        const deleteRes = await getUserManager().deleteUser(user.id)

        res.status(deleteRes).end()
    } catch (e) {
        console.error(e)
        res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
    }
}

const getPersonalizedData = async (req: Request, res: Response, next: NextFunction) => {
    try {
        // @ts-ignore
        const user = req.user as User

        return (
            res
                // @ts-ignore
                .status(ReturnCode.OK)
                .json(user)
        )
    } catch (e) {
        console.error(e)
        res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
    }
}

userRoutes.post("/", createUser)
userRoutes.get("/", checkLoggedIn, getUserData)
userRoutes.get("/personalizeddata", checkLoggedIn, getPersonalizedData)
userRoutes.post("/login", login)
userRoutes.post("/logout", logout)
userRoutes.delete("/", checkLoggedIn, deleteUser)

export default userRoutes
