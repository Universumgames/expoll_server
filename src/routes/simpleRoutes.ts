import { MailRegexRules } from "../entities/entities"
import { ReturnCode } from "expoll-lib"
import express, { NextFunction, Request, Response } from "express"
import getPollManager from "../PollManagement"

// eslint-disable-next-line new-cap
const simpleRoutes = express.Router()

/* const test = async (req: Request, res: Response, next: NextFunction) => {
    return res.status(200).json({
        message: "Test successful"
    })
} */

const pollname = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const pollid = req.params.pollid
        const poll = await getPollManager().getSimplePoll(pollid)

        return res.status(ReturnCode.OK).send(poll?.name)
    } catch {
        return res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
    }
}

const mailRegexList = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const mailRegex = await MailRegexRules.find()

        return res.status(ReturnCode.OK).json({
            regex: mailRegex
        })
    } catch (e) {
        console.error(e)
        res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
    }
}

simpleRoutes.get("/poll/:pollid/title", pollname)
simpleRoutes.get("/mailregex", mailRegexList)

export default simpleRoutes
