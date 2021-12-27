import express, { NextFunction, Request, Response } from "express"
import { config } from "../config"
import { getLoginKey } from "../helper"
import { ReturnCode } from "../interfaces"
import getUserManager from "../UserManagement"
import { checkAdmin, checkLoggedIn } from "./routeHelper"

// eslint-disable-next-line new-cap
const authorizedAdminRoutes = express.Router()

/* const test = async (req: Request, res: Response, next: NextFunction) => {
    return res.status(200).json({
        message: "Test successful"
    })
} */

// eslint-disable-next-line new-cap
const adminRoutes = express.Router()

adminRoutes.use("/", checkLoggedIn, checkAdmin, authorizedAdminRoutes)

export default adminRoutes
