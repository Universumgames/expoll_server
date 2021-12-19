import { ReturnCode, tDate, tDateTime } from "./../interfaces"
import { Poll, PollOptionDate, PollOptionDateTime, PollOptionString } from "./../entities/entities"
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
    try {
        const loginKey = getLoginKey(req)
        const user = await getUserManager().getUser({ loginKey: loginKey })
        if (user == undefined) return res.status(ReturnCode.UNAUTHORIZED).end() // unauthorized
        const body = req.body
        if (body.pollID == undefined) {
            const polls = []
            if (user.polls != undefined) {
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
            }
            return res.status(200).json({ polls: user.polls ?? [] })
        } else {
            const pollID = body.pollID! as string
            const poll = user.polls.find((poll) => poll.id == pollID)
            if (poll == undefined) return res.status(ReturnCode.INVALID_PARAMS).end()

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
            return res.status(ReturnCode.OK).json(returnPoll)
        }
    } catch (e) {
        console.error(e)
        return res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
    }
}

const createPoll = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const loginKey = getLoginKey(req)
        const user = await getUserManager().getUser({ loginKey: loginKey })
        if (user == undefined) return res.status(ReturnCode.INVALID_LOGIN_KEY).end() // unauthorized
        const body = req.body
        if (body.name == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
        const name = body.name as string
        if (body.maxPerUserVoteCount == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
        const maxPerUserVoteCount = body.maxPerUserVoteCount as number
        if (body.type == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
        const type = body.type as number as PollType
        if (body.description == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
        const description = body.description as string
        if (body.options == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
        const options = body.options as any[]

        // save to poll
        const poll = new Poll()
        poll.name = name
        poll.description = description
        poll.type = type
        poll.admin = user
        poll.maxPerUserVoteCount = maxPerUserVoteCount

        const checkedOptions: (PollOptionString | PollOptionDate | PollOptionDateTime)[] = []

        let error = false

        options.forEach((option) => {
            if (type == PollType.String) {
                if (option.value == undefined) error = true
                const o = new PollOptionString()
                o.value = option.value as string
                o.poll = poll
                checkedOptions.push(o)
            } else if (type == PollType.Date) {
                if (option.dateStart == undefined) error = true
                const o = new PollOptionDate()
                o.dateStart = option.dateStart as tDate
                if (option.dateEnd != undefined) o.dateEnd = option.dateEnd as tDate
                o.poll = poll
                checkedOptions.push(o)
            } else if (type == PollType.DateTime) {
                if (option.dateTimeStart == undefined) error = true
                const o = new PollOptionDateTime()
                o.dateTimeStart = option.dateTimeStart as tDateTime
                if (option.dateTimeEnd != undefined) o.dateTimeEnd = option.dateTimeEnd as tDateTime
                o.poll = poll
                checkedOptions.push(o)
            }
        })
        if (error) return res.status(ReturnCode.INVALID_TYPE).end()

        poll.save()
        checkedOptions.forEach((o) => {
            o.save()
        })

        return res.status(ReturnCode.OK).json({
            pollID: poll.id
        })
    } catch (e) {
        console.error(e)
        return res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
    }
}

pollRoutes.get("/", getPolls)
pollRoutes.post("/", createPoll)

// pollRoutes.all("/metaInfo", metaInfo)

export default pollRoutes
