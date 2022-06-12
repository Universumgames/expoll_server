import { addServerTimingsMetrics } from "../helper"
import { ComplexOption, SimpleUserNote } from "expoll-lib/extraInterfaces"
import { config } from "../expoll_config"
import { ReturnCode, tDate, tDateTime, tOptionId, tPollID, tUserID } from "expoll-lib/interfaces"
import {
    Poll,
    PollOption,
    PollOptionDate,
    PollOptionDateTime,
    PollOptionString,
    PollUserNote,
    User,
    Vote
} from "./../entities/entities"
import express, { NextFunction, Request, Response } from "express"
import getPollManager from "../PollManagement"
import getUserManager from "../UserManagement"
import { PollType, VoteValue } from "expoll-lib/interfaces"
import { checkLoggedIn } from "./routeHelper"
import { SimplePoll, SimpleUserVotes } from "expoll-lib/extraInterfaces"
import { DetailedPollResponse, EditPollRequest, PollOverview } from "expoll-lib/requestInterfaces"

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

        // @ts-ignore
        let metrics = req.metrics

        const body = req.body
        if (body.pollID == undefined && req.query.pollID == undefined) {
            // return overview for all polls the user has access to
            let polls: SimplePoll[] = []
            if (user.polls != undefined) {
                const t1 = new Date()
                const pollQueue: Promise<SimplePoll | undefined>[] = []
                for (const poll of user.polls) {
                    pollQueue.push(getPollManager().getSimplePoll(poll.id))
                }
                for (const pollWait of pollQueue) {
                    const poll = await pollWait
                    if (poll != undefined) {
                        polls.push(poll)
                    }
                }
                const t2 = new Date()
                metrics = addServerTimingsMetrics(
                    metrics,
                    "pollSummary",
                    "Collect poll summaries",
                    t2.getTime() - t1.getTime()
                )
            }
            const t3 = new Date()
            // sort by updated
            polls = polls.sort((ele2, ele1) => ele1.lastUpdated.getTime() - ele2.lastUpdated.getTime())
            const t4 = new Date()
            return res
                .set(
                    "Server-Timing",
                    addServerTimingsMetrics(
                        metrics,
                        "sortSummaries",
                        "Sort polls by last updated",
                        t4.getTime() - t3.getTime()
                    )
                )
                .status(ReturnCode.OK)
                .json({ polls: polls } as PollOverview)
        } else {
            const t1 = new Date()
            const pollID = (body.pollID! as tPollID) ?? (req.query.pollID as tPollID)
            if (pollID == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
            const poll = await Poll.findOne({ where: { id: pollID }, relations: ["admin"] })
            if (poll == undefined) return res.status(ReturnCode.INVALID_PARAMS).end()

            const t2 = new Date()

            const constrUsersPromise = getPollManager().getContributedUsers(poll.id)

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

            const t3 = new Date()

            const votes: SimpleUserVotes[] = []
            const constrUsers = await constrUsersPromise
            const userCount = constrUsers.length
            const notes = await getPollManager().getNotes(pollID)
            const easyNotes: SimpleUserNote[] = []

            // add current user if not contributed yet
            if (constrUsers.find((u) => u.id == user.id) == undefined) constrUsers.push(user)

            const t4 = new Date()

            const votePromises: Promise<SimpleUserVotes>[] = []

            const fullPoll = await getPollManager().getPoll(poll.id)

            for (const user of constrUsers) {
                votePromises.push(
                    new Promise(async (resolve, reject) => {
                        const fullVotes = getPollManager().getVotesSync(user.id, fullPoll)
                        // simplify vote structure
                        const vs: { optionID: tOptionId; votedFor?: VoteValue }[] = []
                        for (const v of fullVotes) {
                            vs.push({
                                optionID: v.optionID,
                                votedFor: v.votedFor
                            })
                        }
                        // order votes by options array and fill not voted with blanks
                        const vsFin: { optionID: tOptionId; votedFor?: VoteValue }[] = Array(pollOptions.length)
                        for (let i = 0; i < pollOptions.length; i++) {
                            const optionID = pollOptions[i].id
                            vsFin[i] = vs.find((v) => v.optionID == optionID) ?? { optionID: optionID }
                        }
                        resolve({
                            user: {
                                id: user.id,
                                username: user.username,
                                firstName: user.firstName,
                                lastName: user.lastName
                            },
                            votes: vsFin
                        } as SimpleUserVotes)
                    })
                )

                const n = notes.find((note) => note.user.id == user.id)
                if (n != undefined) {
                    easyNotes.push({
                        user: {
                            id: user.id,
                            username: user.username,
                            firstName: user.firstName,
                            lastName: user.lastName
                        },
                        note: n.note
                    })
                }
            }

            for (const prom of votePromises) {
                votes.push(await prom)
            }

            const t5 = new Date()

            const returnPoll: DetailedPollResponse = {
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
                userVotes: votes,
                allowsMaybe: poll.allowsMaybe,
                userNotes: easyNotes,
                allowsEditing: poll.allowsEditing
            }

            metrics = addServerTimingsMetrics(metrics, "getPoll", "Retrieve Poll Details", t2.getTime() - t1.getTime())
            metrics = addServerTimingsMetrics(metrics, "optionsList", "Get Poll options", t3.getTime() - t2.getTime())
            metrics = addServerTimingsMetrics(
                metrics,
                "various",
                "Get contributed Users and Notes",
                t4.getTime() - t3.getTime()
            )
            metrics = addServerTimingsMetrics(
                metrics,
                "votes",
                "Collect votes and reorganize",
                t5.getTime() - t4.getTime()
            )

            return res.set("Server-Timing", metrics).status(ReturnCode.OK).json(returnPoll)
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
        if (body.allowsMaybe == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
        const allowsMaybe = body.allowsMaybe as boolean
        if (body.allowsEditing == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
        const allowsEditing = body.allowsEditing as boolean

        const pollCount = await getPollManager().getPollCountCreatedByUser(user.id)
        if (pollCount >= config.maxPollCountPerUser && !user.admin) return res.status(ReturnCode.TOO_MANY_POLLS)

        // save to poll
        const poll = new Poll()
        poll.name = name
        poll.description = description
        poll.type = type
        poll.admin = user
        poll.maxPerUserVoteCount = maxPerUserVoteCount
        poll.allowsMaybe = allowsMaybe
        poll.votes = []
        poll.allowsEditing = allowsEditing

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

        const body = req.body as EditPollRequest

        const pollID = body.pollID as string
        if (pollID == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
        const poll = await getPollManager().getPoll(pollID)

        // invite
        if (body.inviteLink != undefined) {
            const inviteLink = body.inviteLink as string
            if (!poll?.allowsEditing) return res.status(ReturnCode.CHANGE_NOT_ALLOWED).end()
            return res.status(await getUserManager().addPoll(user.mail, inviteLink)).end()
        } else if (body.leave != undefined && body.leave) {
            // leave
            if (!poll?.allowsEditing) return res.status(ReturnCode.CHANGE_NOT_ALLOWED).end()
            return res.status(await getUserManager().removeFromPoll(user.id, pollID)).end()
        } else {
            if (poll == undefined) return res.status(ReturnCode.INVALID_PARAMS).end()
            if (poll.admin.id != user.id && !user.admin) return res.status(ReturnCode.UNAUTHORIZED).end()

            // delete poll
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
            const allowsMaybe = (body.allowsMaybe as boolean) ?? undefined
            const allowsEditing = (body.allowsEditing as boolean) ?? undefined
            const userRemove = body.userRemove ?? undefined
            const votes = body.votes ?? undefined
            const options = body.options as ComplexOption[]
            const notes = (body.notes as { userID: tUserID; note: string }[]) ?? []
            // updating simple settings
            if (name != undefined) poll.name = name
            if (description != undefined) poll.description = description
            // constrain maxperuservotecount to -1
            if (maxPerUserVoteCount != undefined)
                poll.maxPerUserVoteCount = maxPerUserVoteCount <= -1 ? -1 : maxPerUserVoteCount
            if (allowsMaybe != undefined) poll.allowsMaybe = allowsMaybe
            if (allowsEditing != undefined) poll.allowsEditing = allowsEditing

            if (!poll.allowsEditing) {
                await poll.save()
                return res.status(ReturnCode.OK).end()
            }

            if (userRemove != undefined) {
                // remove user from poll
                for (const userID of userRemove) {
                    /* if (userID == undefined) continue
                    const user = await getUserManager().getUser({ userID: userID })
                    if (user == undefined) continue
                    user.polls = user.polls.filter((poll) => poll.id != pollID)
                    await user.save()
                    await Vote.delete({ poll: poll, user: user }) */
                    await getUserManager().removeFromPoll(userID, pollID)
                }
            }

            // edit votes
            if (votes != undefined) {
                // async vote saving
                const votePromiseArray: Promise<void>[] = []
                for (const vote of votes) {
                    if (vote == undefined || vote.userID == undefined) continue
                    votePromiseArray.push(
                        new Promise(async (resolve, reject) => {
                            const user = await getUserManager().getUser({ userID: vote.userID })
                            if (user == undefined) {
                                resolve()
                                return
                            }
                            const v = user.votes.find((vot) => vot.optionID == vote.optionID)
                            if (v == undefined) return
                            v.votedFor = vote.votedFor
                            await v.save()
                            resolve()
                        })
                    )
                }
                for (const prom of votePromiseArray) {
                    await prom
                }
            }

            // remove or add options to poll
            if (options != undefined) {
                for (const option of options) {
                    // delete option
                    if (option.id != undefined) {
                        switch (poll.type) {
                            case PollType.String:
                                await PollOptionString.delete({ poll: poll, id: option.id })
                                break
                            case PollType.Date:
                                await PollOptionDate.delete({ poll: poll, id: option.id })
                                break
                            case PollType.DateTime:
                                await PollOptionDateTime.delete({ poll: poll, id: option.id })
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
                    if (!error) {
                        // async option saving
                        const saveOptionPromises: Promise<any>[] = []
                        for (const opt of checkedOptions) {
                            saveOptionPromises.push(opt.save())
                        }
                        for (const prom of saveOptionPromises) {
                            await prom
                        }
                    }
                }
            }

            await poll.save()

            if (notes != undefined) {
                for (const note of notes) {
                    const user = await getUserManager().getUser({ userID: note.userID })
                    if (user == undefined) continue
                    const oldNote = await PollUserNote.findOne({
                        where: { user: user, poll: poll }
                    })
                    if (oldNote != undefined) {
                        oldNote.note = note.note
                        await oldNote.save()
                    } else {
                        const n = new PollUserNote()
                        n.note = note.note
                        n.user = user
                        n.poll = poll

                        await n.save()
                    }
                }
            }

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
