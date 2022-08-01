import { NextFunction, Request, Response } from "express"
import { Session, User } from "../entities/entities"
import { addServerTimingsMetrics, cookieName, getLoginKey, isAdmin } from "../helper"
import { ReturnCode } from "expoll-lib/interfaces"
import getUserManager from "../UserManagement"

export const checkLoggedIn = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const t1 = new Date()
        const loginKey = getLoginKey(req)
        if (loginKey == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
        const t2 = new Date()
        // @ts-ignore
        req.loginKey = loginKey
        const userReq = getUserManager().getUser({ loginKey: loginKey })

        const t3 = new Date()
        const user = await userReq
        const t4 = new Date()
        if (user == undefined) {
            return res.status(ReturnCode.INVALID_LOGIN_KEY).cookie(cookieName, {}).end() // unauthorized
        }

        const session = await Session.findOne({ where: { loginKey: loginKey } })
        if (session && session.userAgent == undefined) {
            session.userAgent = req.headers["user-agent"] ?? "unknown"
            await session.save()
        }

        user.admin = getUserManager().userIsAdminOrSuperAdminSync(user)

        // setting user and loginkey for methods down the line
        // @ts-ignore
        req.user = user

        let metrics = addServerTimingsMetrics("", "loginKey", "Check login key", t2.getTime() - t1.getTime())
        metrics = addServerTimingsMetrics(metrics, "userAgent", "Update user agent", t3.getTime() - t2.getTime())
        metrics = addServerTimingsMetrics(
            metrics,
            "userData",
            "Get user data from Database",
            t4.getTime() - t3.getTime()
        )
        // @ts-ignore
        req.metrics = metrics

        // TODO possible performance issue
        // check all session if expired
        new Promise<void>(async (resolve, reject) => {
            // get all sessions from user
            const sessions = await Session.find({ where: { user: user } })
            // check if any session is expired
            for (const session of sessions) {
                if (session.expiration < new Date()) {
                    // delete session
                    await session.remove()
                }
            }
            resolve()
        })

        next()
    } catch (e) {
        console.error(e)
    }
}

export const checkAdmin = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const t1 = new Date()
        // @ts-ignore
        let user: User | undefined = req.user as User
        if (user == undefined) {
            const loginKey = getLoginKey(req)
            if (loginKey == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
            user = await getUserManager().getUser({ loginKey: loginKey })
            if (user == undefined) return res.status(ReturnCode.INVALID_LOGIN_KEY).end()
            user.admin = isAdmin(user)
        }
        // check for "normal" admin or superadmin
        if (user == undefined || !user.admin) return res.status(ReturnCode.NOT_ACCEPTABLE).end()

        const t2 = new Date()
        // @ts-ignore
        req.metrics = addServerTimingsMetrics(
            // @ts-ignore
            req.metrics != undefined ? req.metrics : "",
            "checkAdmin",
            "Check if user is an Admin",
            t2.getTime() - t1.getTime()
        )

        next()
    } catch (e) {
        console.error(e)
    }
}
