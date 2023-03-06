import { config } from "./../expoll_config"
import { ReturnCode } from "expoll-lib/interfaces"
import express, { NextFunction, Request, Response } from "express"
import adminRoutes from "./adminRoutes"
import pollRoutes from "./pollRoutes"
import userRoutes from "./userRoutes"
import voteRoutes from "./voteRoutes"
import simpleRoutes from "./simpleRoutes"
import authRoutes from "./authentication/auth"
import notificationRoutes from "./notifications/notifications"
import { compareVersion, getDataFromAny } from "../helper"

// eslint-disable-next-line new-cap
const apiRoutes = express.Router()
apiRoutes.use("/user", userRoutes)
apiRoutes.use("/poll", pollRoutes)
apiRoutes.use("/vote", voteRoutes)
apiRoutes.use("/admin", adminRoutes)
apiRoutes.use("/auth", authRoutes)
apiRoutes.use("/simple", simpleRoutes)
apiRoutes.use("/notifications", notificationRoutes)

const test = async (req: Request, res: Response, next: NextFunction) => {
    return res.status(200).json({
        message: "Test successful"
    })
}

export const metaInfo = async (req: Request, res: Response, next: NextFunction) => {
    const returnData = {
        body: req.body ?? "",
        headers: req.headers,
        cookies: req.cookies,
        signedCookies: req.signedCookies,
        url: req.url,
        path: req.path,
        method: req.method,
        protocol: req.protocol,
        route: req.route,
        params: req.params,
        hostname: req.hostname,
        ip: req.ip,
        httpVersion: req.httpVersion,
        secure: req.secure,
        subdomains: req.subdomains,
        xhr: req.xhr,
        serverInfo: {
            version: config.serverVersion,
            serverPort: config.serverPort,
            frontendPort: config.frontEndPort,
            loginLinkBase: config.loginLinkURL,
            mailSender: config.mail.mailUser
        }
    }
    return res.status(ReturnCode.OK).json(returnData)
}

export const serverInfo = async (req: Request, res: Response, next: NextFunction) => {
    const returnData = {
        version: config.serverVersion,
        minimumRequiredVersion: config.minimumRequiredClientVersion,
        serverPort: config.serverPort,
        frontendPort: config.frontEndPort,
        loginLinkBase: config.loginLinkURL,
        mailSender: config.mail.mailUser
    }
    return res.status(ReturnCode.OK).json(returnData)
}

export const checkCompliance = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const clientVersion = getDataFromAny(req, "version") as string
        return res.status(ReturnCode.OK).send(`${compareVersion(clientVersion, config.minimumRequiredClientVersion)}`)
    } catch (err) {
        console.warn(err)
        return res.status(ReturnCode.INTERNAL_SERVER_ERROR).json({
            message: err
        })
    }
}

apiRoutes.get("/test", test)
apiRoutes.all("/metaInfo", metaInfo)
apiRoutes.all("/serverInfo", serverInfo)
apiRoutes.all("/compliance", checkCompliance)

export default apiRoutes
