import { Vote } from "./../entities/entities"
import { ReturnCode, tOptionId } from "./../interfaces"
import express, { NextFunction, Request, Response } from "express"
import { getLoginKey } from "../helper"
import getUserManager from "../UserManagement"
import getPollManager from "../PollManagement"

// eslint-disable-next-line new-cap
const voteRoutes = express.Router()

/* const test = async (req: Request, res: Response, next: NextFunction) => {
    return res.status(200).json({
        message: "Test successful"
    })
} */

// voteRoutes.all("/metaInfo", metaInfo)

const createVote = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const loginKey = getLoginKey(req)
        const user = await getUserManager().getUser({ loginKey: loginKey })
        if (user == undefined) return res.status(ReturnCode.INVALID_LOGIN_KEY).end() // unauthorized
        const body = req.body
        if (body.pollID == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
        const pollID = body.pollID as string
        if (body.optionID == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
        const optionID = body.optionID as tOptionId
        if (body.votedFor == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
        const votedFor = body.votedFor as boolean

        const poll = await getPollManager().getPoll(pollID)
        if (poll == undefined) return res.status(ReturnCode.INVALID_PARAMS).end()

        let vote = await getPollManager().getVote(user.id, poll.id, optionID)

        if (
            (await getPollManager().getVoteCountFromUser(user.id, pollID)) <= poll.maxPerUserVoteCount ||
            vote != undefined
        ) {
            if (vote == undefined) vote = new Vote()
            vote.user = user
            vote.optionID = optionID
            vote.poll = poll
            vote.votedFor = votedFor

            vote.save()
        }

        return res.status(ReturnCode.OK).end()
    } catch (e) {
        console.error(e)
        return res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
    }
}

const revokeVote = async (req: Request, res: Response, next: NextFunction) => {
    return res.status(200).json({
        message: "Test successful"
    })
}

voteRoutes.post("/vote", createVote)
voteRoutes.delete("/vote", revokeVote)

export default voteRoutes
