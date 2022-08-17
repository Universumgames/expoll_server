import { Session } from "../../entities/session"
import { User } from "../../entities/user"
import { ReturnCode } from "expoll-lib"
import express, { NextFunction, Request, Response } from "express"
import { cookieName } from "../../helper"
import { checkLoggedIn } from "../../routes/routeHelper"
import simpleAuthRoutes from "./simple"
import webauthnRoutes from "./webauthn"

// eslint-disable-next-line new-cap
const authRoutes = express.Router()

/* const test = async (req: Request, res: Response, next: NextFunction) => {
    return res.status(200).json({
        message: "Test successful"
    })
} */

const logout = async (req: Request, res: Response, next: NextFunction) => {
    try {
        // @ts-ignore
        const user = req.user as User
        // @ts-ignore
        const loginKey = req.loginKey as string

        const shortKey = req.body.shortKey as string
        if (shortKey == undefined) {
            const session = await Session.findOne({ where: { loginKey: loginKey } })
            if (session != undefined) {
                await session.remove()
                res.status(ReturnCode.OK).cookie(cookieName, "", { httpOnly: true, sameSite: "strict" }).end()
            } else res.status(ReturnCode.INVALID_LOGIN_KEY).end()
        } else {
            const sessions = await Session.find({ where: { user: user } })

            const session = await sessions.find((session) => session.loginKey.startsWith(shortKey))

            if (session) {
                await session.remove()
                res.status(ReturnCode.OK).end()
            } else res.status(ReturnCode.INVALID_LOGIN_KEY).end()
        }
    } catch (e) {
        console.error(e)
        res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
    }
}

const logoutAll = async (req: Request, res: Response, next: NextFunction) => {
    try {
        // @ts-ignore
        const user = req.user as User

        // const sessions = await getUserManager().getSessions(user.id)
        await Session.delete({ user: user })
        res.status(ReturnCode.OK).cookie(cookieName, "", { httpOnly: true, sameSite: "strict" }).end()
    } catch (e) {
        console.error(e)
        res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
    }
}

authRoutes.post("/logoutAll", checkLoggedIn, logoutAll)
authRoutes.post("/logout", checkLoggedIn, logout)

authRoutes.use("/simple", simpleAuthRoutes)
authRoutes.use("/webauthn", webauthnRoutes)

export default authRoutes
