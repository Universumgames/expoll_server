import { ReturnCode, tDate, tDateTime, tOptionId, tPollID, tUserID } from "./../interfaces"
import {
    Poll,
    PollOption,
    PollOptionDate,
    PollOptionDateTime,
    PollOptionString,
    User,
    Vote
} from "./../entities/entities"
import express, { NextFunction, Request, Response } from "express"
import getPollManager from "../PollManagement"
import getUserManager from "../UserManagement"
import { PollType } from "../interfaces"
import { checkLoggedIn } from "./routeHelper"

// eslint-disable-next-line new-cap
const pollRoutes = express.Router()

/* const test = async (req: Request, res: Response, next: NextFunction) => {
    return res.status(200).json({
        message: "Test successful"
    })
} */

const getPolls = async (req: Request, res: Response, next: NextFunction) => {
    try {
        // @ts-ignore
        const user = req.user as User
        /* const loginKey = getLoginKey(req)
        const user = await getUserManager().getUser({ loginKey: loginKey })
        if (user == undefined) return res.status(ReturnCode.UNAUTHORIZED).end() // unauthorized */
        const body = req.body
        if (body.pollID == undefined && req.query.pollID == undefined) {
            // return overview for all polls the user has access to
            let polls: any[] = []
            if (user.polls != undefined) {
                for (const poll of user.polls) {
                    const userCount = (await getPollManager().getContributedUsers(poll.id)).length
                    // simplify and constrain "access" to polls
                    const pollAdd = {
                        admin: {
                            firstName: poll.admin.firstName,
                            lastName: poll.admin.lastName,
                            username: poll.admin.username,
                            id: poll.admin.id
                        },
                        name: poll.name,
                        description: poll.description,
                        userCount: userCount,
                        lastUpdated: poll.updated,
                        type: poll.type as number,
                        id: poll.id
                    }
                    polls.push(pollAdd)
                }
            }
            // sort by updated
            polls = polls.sort((ele2, ele1) => ele1.lastUpdated - ele2.lastUpdated)
            return res.status(ReturnCode.OK).json({ polls: polls })
        } else {
            const pollID = (body.pollID! as tPollID) ?? (req.query.pollID as tPollID)
            if (pollID == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
            const poll = await Poll.findOne({ where: { id: pollID }, relations: ["admin"] })
            if (poll == undefined) return res.status(ReturnCode.INVALID_PARAMS).end()

            const constrUsers = await getPollManager().getContributedUsers(poll.id)

            const userCount = constrUsers.length
            let pollOptions: PollOption[] = []
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

            // sort options by id
            pollOptions = pollOptions.sort((n1, n2) => n1.id - n2.id)

            const votes: {
                user: {
                    id: number
                    username: string
                    mail: string
                    firstName: string
                    lastName: string
                    admin: boolean
                }
                votes: { optionID: tOptionId; votedFor?: boolean }[]
            }[] = []
            // add current user if not constributed yet
            if (constrUsers.find((u) => u.id == user.id) == undefined) constrUsers.push(user)

            for (const user of constrUsers) {
                const fullVotes = await getPollManager().getVotes(user.id, poll.id)
                // simplify vote structure
                const vs: { optionID: tOptionId; votedFor?: boolean }[] = []
                for (const v of fullVotes) {
                    vs.push({
                        optionID: v.optionID,
                        votedFor: v.votedFor
                    })
                }
                // order votes by options array and fill not voted with blanks
                const vsFin: { optionID: tOptionId; votedFor?: boolean }[] = Array(pollOptions.length)
                for (let i = 0; i < pollOptions.length; i++) {
                    const optionID = pollOptions[i].id
                    vsFin[i] = vs.find((v) => v.optionID == optionID) ?? { optionID: optionID }
                }
                votes.push({
                    user: {
                        id: user.id,
                        username: user.username,
                        mail: user.mail,
                        firstName: user.firstName,
                        lastName: user.lastName,
                        admin: await getUserManager().userIsAdminOrSuperAdmin(user.id)
                    },
                    votes: vsFin
                })
            }

            const returnPoll = {
                pollID: pollID,
                admin: {
                    firstName: poll.admin.firstName,
                    lastName: poll.admin.lastName,
                    username: poll.admin.username,
                    id: poll.admin.id
                },
                name: poll.name,
                description: poll.description,
                maxPerUserVoteCount: poll.maxPerUserVoteCount,
                userCount: userCount,
                lastUpdated: poll.updated,
                created: poll.created,
                type: poll.type,
                options: pollOptions,
                userVotes: votes
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
        // @ts-ignore
        const user = req.user as User
        /* const loginKey = getLoginKey(req)
        const user = await getUserManager().getUser({ loginKey: loginKey })
        if (user == undefined) return res.status(ReturnCode.INVALID_LOGIN_KEY).end() // unauthorized */
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
        poll.votes = []

        const checkedOptions: PollOption[] = []

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

        await poll.save()
        for (const opt of checkedOptions) {
            await opt.save()
        }
        if (user.polls == undefined) user.polls = []
        user.polls.push(poll)
        await user.save()

        return res.status(ReturnCode.OK).json({
            pollID: poll.id
        })
    } catch (e) {
        console.error(e)
        return res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
    }
}

const editPoll = async (req: Request, res: Response, next: NextFunction) => {
    try {
        // @ts-ignore
        const user = req.user as User
        /* const loginKey = getLoginKey(req)
        const user = await getUserManager().getUser({ loginKey: loginKey })
        if (user == undefined) return res.status(ReturnCode.INVALID_LOGIN_KEY).end() // unauthorized */
        const body = req.body

        // invite
        if (body.inviteLink != undefined) {
            const inviteLink = body.inviteLink as string
            return res.status(await getUserManager().addPoll(user.mail, inviteLink)).end()
        } else {
            const pollID = body.pollID as string
            if (pollID == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
            const poll = await getPollManager().getPoll(pollID)

            if (poll == undefined) return res.status(ReturnCode.INVALID_PARAMS).end()
            if (poll.admin.id != user.id && !user.admin) return res.status(ReturnCode.UNAUTHORIZED).end()

            if (body.delete != undefined && (body.delete as boolean) == true) {
                // poll.votes = []
                // await poll.save()

                const pollVotes = await Vote.find({ where: { poll: poll } })
                for (const vote of pollVotes) {
                    await Vote.remove(vote)
                }
                let pollOptions: any[] = []
                switch (poll.type) {
                    case PollType.String: {
                        pollOptions = await PollOptionString.find({ where: { poll: poll } })
                        for (const option of pollOptions) {
                            await PollOptionString.delete(option)
                        }
                        break
                    }
                    case PollType.Date: {
                        pollOptions = await PollOptionDate.find({ where: { poll: poll } })
                        for (const option of pollOptions) {
                            await PollOptionDate.delete(option)
                        }
                        break
                    }
                    case PollType.DateTime: {
                        pollOptions = await PollOptionDateTime.find({ where: { poll: poll } })
                        for (const option of pollOptions) {
                            await PollOptionDateTime.delete(option)
                        }
                        break
                    }
                }

                await Poll.remove(poll)
                return res.status(ReturnCode.OK).end()
            }

            const name = (body.name as string) ?? undefined
            const description = (body.description as string) ?? undefined
            const maxPerUserVoteCount = (body.maxPerUserVoteCount as number) ?? undefined
            const userRemove = (body.userRemove as number[]) ?? undefined
            const votes = (body.votes as { userID: tUserID; optionID: tOptionId; votedFor: boolean }[]) ?? undefined
            const options = body.options as {
                optionID?: tOptionId
                value?: string
                dateStart?: Date
                dateEnd?: Date
                dateTimeStart?: Date
                dateTimeEnd?: Date
            }[]
            // updating simple settings
            if (name != undefined) poll.name = name
            if (description != undefined) poll.description = description
            // constrain maxperuservotecount to -1
            if (maxPerUserVoteCount != undefined)
                poll.maxPerUserVoteCount = maxPerUserVoteCount <= -1 ? -1 : maxPerUserVoteCount

            if (userRemove != undefined) {
                // remove user from poll
                for (const userID of userRemove) {
                    const user = await getUserManager().getUser({ userID: userID })
                    if (user == undefined) continue
                    user.polls = user.polls.filter((poll) => poll.id != pollID)
                    await user.save()
                    await Vote.delete({ poll: poll, user: user })
                }
            }

            // edit votes
            if (votes != undefined) {
                for (const vote of votes) {
                    const user = await getUserManager().getUser({ userID: vote.userID })
                    if (user == undefined) continue
                    const v = user.votes.find((vot) => vot.optionID == vote.optionID)
                    if (v == undefined) return
                    v.votedFor = vote.votedFor
                    await v.save()
                }
            }

            // remove or add options to poll
            if (options != undefined) {
                for (const option of options) {
                    // delete option
                    if (option.optionID != undefined) {
                        switch (poll.type) {
                            case PollType.String:
                                await PollOptionString.delete({ poll: poll, id: option.optionID })
                                break
                            case PollType.Date:
                                await PollOptionDate.delete({ poll: poll, id: option.optionID })
                                break
                            case PollType.DateTime:
                                await PollOptionDateTime.delete({ poll: poll, id: option.optionID })
                                break
                        }
                        continue
                    }

                    // add option
                    const type = poll.type
                    let error = false
                    const checkedOptions: PollOption[] = []
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
                    if (!error)
                        for (const opt of checkedOptions) {
                            await opt.save()
                        }
                }
            }

            await poll.save()

            return res.status(ReturnCode.OK).end()
        }
    } catch (e) {
        console.error(e)
        return res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
    }
}

pollRoutes.get("/", checkLoggedIn, getPolls)
pollRoutes.post("/", checkLoggedIn, createPoll)
pollRoutes.put("/", checkLoggedIn, editPoll)

// pollRoutes.all("/metaInfo", metaInfo)

export default pollRoutes
