import { Poll } from "./../entities/entities"
import express, { NextFunction, Request, Response } from "express"
import { getLoginKey } from "../helper"
import getPollManager from "../PollManagement"
import Router from "../router"
import getUserManager from "../UserManagement"
import { PollType } from "../interfaces"

// eslint-disable-next-line new-cap
const pollRoutes = express.Router()

/* const test = async (req: Request, res: Response, next: NextFunction) => {
    return res.status(200).json({
        message: "Test successful"
    })
} */

const getPolls = async (req: Request, res: Response, next: NextFunction) => {
    const loginKey = getLoginKey(req)
    const user = await getUserManager().getUser({ loginKey: loginKey })
    if (user == undefined) return res.status(401).end() // unauthorized
    const body = req.body
    if (body.pollID == undefined) {
        const polls = []
        await user.polls.forEach(async (poll) => {
            const userCount = (await getPollManager().getContributedUsers(poll.id)).length
            const pollAdd = {
                admin: {
                    firstName: poll.admin.firstName,
                    lastName: poll.admin.lastName,
                    username: poll.admin.username
                },
                description: poll.description,
                userCount: userCount,
                lastUpdated: poll.updated,
                type: poll.type as number
            }
            polls.push(pollAdd)
        })
        return res.status(200).json({ polls: user.polls ?? [] })
    } else {
        const pollID = body.pollID! as string
        const poll = user.polls.find((poll) => poll.id == pollID)
        if (poll == undefined) return res.status(400).end()

        const userCount = (await getPollManager().getContributedUsers(poll.id)).length
        let pollOptions = []
        switch (poll.type) {
            case PollType.String:
                pollOptions = (await getPollManager().getStringPollOptions(poll.id))!
                break
            case PollType.Date:
                pollOptions = (await getPollManager().getDatePollOptions(poll.id))!
                break
            case PollType.DateTime:
                pollOptions = (await getPollManager().getDateTimePollOptions(poll.id))!
                break
        }

        const returnPoll = {
            pollID: pollID,
            admin: {
                firstName: poll.admin.firstName,
                lastName: poll.admin.lastName,
                username: poll.admin.username
            },
            description: poll.description,
            userCount: userCount,
            lastUpdated: poll.updated,
            created: poll.created,
            type: poll.created,
            options: pollOptions
        }
        return res.status(200).json(returnPoll)
    }
}

pollRoutes.get("/", getPolls)

// pollRoutes.all("/metaInfo", metaInfo)

export default pollRoutes
