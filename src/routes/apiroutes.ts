import express, { NextFunction, Request, Response } from "express"
import pollRoutes from "./pollRoutes"
import userRoutes from "./userRoutes"

// eslint-disable-next-line new-cap
const apiRoutes = express.Router()
apiRoutes.use("/user", userRoutes)
apiRoutes.use("/poll", pollRoutes)

const test = async (req: Request, res: Response, next: NextFunction) => {
    return res.status(200).json({
        message: "Test successful"
    })
}

const metaInfo = async (req: Request, res: Response, next: NextFunction) => {
    const returnData = {
        body: req.body ?? "",
        headers: req.headers,
        cookies: req.cookies,
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
    return res.status(200).json(returnData)
}

apiRoutes.get("/test", test)
// apiRoutes.get("/metaInfo", metaInfo)
apiRoutes.all("/metaInfo", metaInfo)

export default apiRoutes
