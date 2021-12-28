import { checkLoggedIn } from "./routeHelper"
import { User, Vote } from "./../entities/entities"
import { ReturnCode, tOptionId, tUserID } from "./../interfaces"
import express, { NextFunction, Request, Response } from "express"
import getPollManager from "../PollManagement"
import getUserManager from "../UserManagement"

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
        // @ts-ignore
        const user = req.user as User
        /* const loginKey = getLoginKey(req)
        const user = await getUserManager().getUser({ loginKey: loginKey })
        if (user == undefined) return res.status(ReturnCode.INVALID_LOGIN_KEY).end() // unauthorized */
        const body = req.body
        if (body.pollID == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
        const pollID = body.pollID as string
        if (body.optionID == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
        const optionID = body.optionID as tOptionId
        if (body.votedFor == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
        const votedFor = body.votedFor as boolean

        const poll = await getPollManager().getPoll(pollID)
        if (poll == undefined) return res.status(ReturnCode.INVALID_PARAMS).end()

        // this part is for when an admin want to change a vote
        let userIDToUse = user.id
        if ((poll.admin.id == user.id || user.admin) && body.userID != undefined) userIDToUse = body.userID as tUserID
        const u2 = await getUserManager().getUser({ userID: userIDToUse })
        if (u2 == undefined) return res.status(ReturnCode.INVALID_PARAMS).end()

        // get old vote to change it when exists
        let vote: Vote | undefined = await getPollManager().getVote(userIDToUse, pollID, optionID)

        // check if an additional vote could be made
        const count = await getPollManager().getVoteCountFromUser(userIDToUse, pollID)

        if (
            count + (votedFor ? 1 : 0) <= poll.maxPerUserVoteCount ||
            (vote != undefined && !votedFor) ||
            poll.maxPerUserVoteCount == -1
        ) {
            if (vote == undefined) vote = new Vote()
            vote.user = userIDToUse == user.id ? user : u2
            vote.optionID = optionID
            vote.poll = poll
            vote.votedFor = votedFor

            await vote.save()
            poll.updated = new Date()
            if (poll.votes == undefined) poll.votes = []
            poll.votes.push(vote)
            await poll.save()
        } else return res.status(ReturnCode.NOT_ACCEPTABLE).end()

        return res.status(ReturnCode.OK).end()
    } catch (e) {
        console.error(e)
        return res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
    }
}

/* const revokeVote = async (req: Request, res: Response, next: NextFunction) => {
    return res.status(200).json({
        message: "Test successful"
    })
} */

voteRoutes.post("/", checkLoggedIn, createVote)
// voteRoutes.delete("/vote", revokeVote)

export default voteRoutes
