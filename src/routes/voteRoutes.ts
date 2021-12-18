import express, { NextFunction, Request, Response } from "express"

// eslint-disable-next-line new-cap
const voteRoutes = express.Router()

/* const test = async (req: Request, res: Response, next: NextFunction) => {
    return res.status(200).json({
        message: "Test successful"
    })
} */

// voteRoutes.all("/metaInfo", metaInfo)

export default voteRoutes
