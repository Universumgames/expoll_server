import express, { NextFunction, Request, Response } from "express"
import adminRoutes from "./adminRoutes"
import pollRoutes from "./pollRoutes"
import userRoutes from "./userRoutes"
import voteRoutes from "./voteRoutes"

// eslint-disable-next-line new-cap
const apiRoutes = express.Router()
apiRoutes.use("/user", userRoutes)
apiRoutes.use("/poll", pollRoutes)
apiRoutes.use("/vote", voteRoutes)
apiRoutes.use("/admin", adminRoutes)

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
        test: "test message"
    }
    return res
        .status(200)
        .cookie("test-cookie-name", "test-cookie-value", { sameSite: true })
        .cookie("test2", "testval", { sameSite: true })
        .json(returnData)
}

apiRoutes.get("/test", test)
apiRoutes.all("/metaInfo", metaInfo)

export default apiRoutes
