import express, { NextFunction, Request, Response } from "express"
import Router from "../router"

// eslint-disable-next-line new-cap
const pollRoutes = express.Router()

/* const test = async (req: Request, res: Response, next: NextFunction) => {
    return res.status(200).json({
        message: "Test successful"
    })
} */

// pollRoutes.all("/metaInfo", metaInfo)

export default pollRoutes
