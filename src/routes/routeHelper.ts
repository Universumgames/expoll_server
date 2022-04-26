import { NextFunction, Request, Response } from "express"
import { User } from "../entities/entities"
import { addServerTimingsMetrics, cookieName, getLoginKey, isAdmin } from "../helper"
import { ReturnCode } from "expoll-lib/interfaces"
import getUserManager from "../UserManagement"

export const checkLoggedIn = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const t1 = new Date()
        const loginKey = getLoginKey(req)
        const t2 = new Date()
        if (loginKey == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
        const user = await getUserManager().getUser({ loginKey: loginKey })
        const t3 = new Date()
        if (user == undefined) {
            return res.status(ReturnCode.INVALID_LOGIN_KEY).cookie(cookieName, {}).end() // unauthorized
        }

        user.admin = await getUserManager().userIsAdminOrSuperAdmin(user.id)

        // setting user and loginkey for methods down the line
        // @ts-ignore
        req.user = user
        // @ts-ignore
        req.loginKey = loginKey

        let metrics = addServerTimingsMetrics("", "loginKey", "Check login key", t2.getTime() - t1.getTime())
        metrics = addServerTimingsMetrics(
            metrics,
            "userData",
            "Get user data from Database",
            t3.getTime() - t2.getTime()
        )
        // @ts-ignore
        req.metrics = metrics

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
