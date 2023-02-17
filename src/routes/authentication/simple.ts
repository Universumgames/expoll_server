import { ReturnCode } from "expoll-lib"
import { config } from "../../expoll_config"
import express, { NextFunction, Request, Response } from "express"
import { cookieConfig, cookieName, getLoginKey } from "../../helper"
import getUserManager from "../../UserManagement"

// eslint-disable-next-line new-cap
const simpleAuthRoutes = express.Router()

/* const test = async (req: Request, res: Response, next: NextFunction) => {
    return res.status(200).json({
        message: "Test successful"
    })
} */

const login = async (req: Request, res: Response, next: NextFunction) => {
    try {
        // @ts-ignore
        const loginKey = getLoginKey(req)
        if (loginKey == undefined) {
            return res.status(await getUserManager().sendLoginMail(req.body.mail, req)).end()
        }
        let user = await getUserManager().getUser({ loginKey: loginKey })
        if (user == undefined && loginKey == config.testUser.loginKey) {
            user = await getUserManager().ensureTestUser()
        }
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

simpleAuthRoutes.post("/", login)

export default simpleAuthRoutes
