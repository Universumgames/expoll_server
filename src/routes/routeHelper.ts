import { NextFunction, Request, Response } from "express"
import { User } from "../entities/entities"
import { cookieName, getLoginKey, isAdmin } from "../helper"
import { ReturnCode } from "expoll-lib/interfaces"
import getUserManager from "../UserManagement"

export const checkLoggedIn = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const loginKey = getLoginKey(req)
        if (loginKey == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
        const user = await getUserManager().getUser({ loginKey: loginKey })
        if (user == undefined) {
            return res.status(ReturnCode.INVALID_LOGIN_KEY).cookie(cookieName, {}).end() // unauthorized
        }

        user.admin = await getUserManager().userIsAdminOrSuperAdmin(user.id)

        // setting user and loginkey for methods down the line
        // @ts-ignore
        req.user = user
        // @ts-ignore
        req.loginKey = loginKey

        next()
    } catch (e) {
        console.error(e)
    }
}

export const checkAdmin = async (req: Request, res: Response, next: NextFunction) => {
    try {
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
        if (user == undefined || !user.admin) return res.status(ReturnCode.INVALID_LOGIN_KEY).end()

        next()
    } catch (e) {
        console.error(e)
    }
}
