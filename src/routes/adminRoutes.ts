import { UserInfo } from "expoll-lib/adminInterfaces"
import express, { NextFunction, Request, Response } from "express"
import { config } from "../expoll_config"
import { ReturnCode } from "expoll-lib/interfaces"
import getPollManager from "../PollManagement"
import getUserManager from "../UserManagement"
import { checkAdmin, checkLoggedIn } from "./routeHelper"
import { AdminPollListResponse, AdminUserListResponse, AdminEditUserRequest } from "expoll-lib/requestInterfaces"
import { SimplePoll } from "expoll-lib/extraInterfaces"
import { Session, User } from "../entities/entities"
import { isSuperAdmin } from "../helper"
import { visitEachChild } from "typescript"

// eslint-disable-next-line new-cap
const authorizedAdminRoutes = express.Router()

/* const test = async (req: Request, res: Response, next: NextFunction) => {
    return res.status(200).json({
        message: "Test successful"
    })
} */

const getUsers = async (req: Request, res: Response, next: NextFunction) => {
    try {
        // @ts-ignore
        // const adminUser = req.user as User

        const users = await getUserManager().getUsers()

        const cleanedUsers: UserInfo[] = []

        for (const user of users) {
            cleanedUsers.push({
                id: user.id,
                username: user.username,
                firstName: user.firstName,
                lastName: user.lastName,
                mail: user.mail,
                admin: user.admin || user.mail == config.superAdminMail,
                superAdmin: user.mail == config.superAdminMail,
                active: user.active
            })
        }

        const data: AdminUserListResponse = {
            users: cleanedUsers,
            totalCount: cleanedUsers.length
        }
        return res.status(ReturnCode.OK).json(data)
    } catch (e) {
        console.error(e)
        return res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
    }
}

const editUser = async (req: Request, res: Response, next: NextFunction) => {
    try {
        // @ts-ignore
        const admin = req.user as User
        const editReq = req.body as AdminEditUserRequest
        if (editReq.userID == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()

        const editUser = await getUserManager().getUser({ userID: editReq.userID })
        if (editUser == undefined) return res.status(ReturnCode.INVALID_PARAMS).end()

        if (editReq.delete != undefined && editReq.delete) {
            const deleteRes = await getUserManager().deleteUser(editUser.id)
            return res.status(deleteRes).end()
        }

        if (editReq.firstName != undefined) editUser.firstName = editReq.firstName

        if (editReq.lastName != undefined) editUser.lastName = editReq.lastName

        if (editReq.mail != undefined) editUser.mail = editReq.mail

        // promote to admin, demote only if current user is super admin
        if (editReq.admin != undefined) {
            if (!isSuperAdmin(admin) && !editReq.admin) {
                return res.status(ReturnCode.UNAUTHORIZED).end()
            }
            editUser.admin = editReq.admin || isSuperAdmin(editUser)
        }

        await editUser.save()

        return res.status(ReturnCode.OK).end()
    } catch (e) {
        console.error(e)
        return res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
    }
}

const getPolls = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const allPolls = await getPollManager().getPolls()
        let polls: SimplePoll[] = []
        for (const poll of allPolls) {
            const userCount = (await getPollManager().getContributedUsers(poll.id)).length
            // simplify and constrain "access" to polls
            const pollAdd: SimplePoll = {
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
                pollID: poll.id
            }
            polls.push(pollAdd)
        }
        // sort by updated
        polls = polls.sort((ele2, ele1) => ele1.lastUpdated.getTime() - ele2.lastUpdated.getTime())
        const data: AdminPollListResponse = { polls: polls, totalCount: polls.length }
        return res.status(ReturnCode.OK).json(data)
    } catch (e) {
        console.error(e)
        return res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
    }
}

const deleteUser = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const userID = req.body.userID

        const deleteRes = await getUserManager().deleteUser(userID)

        res.status(deleteRes).end()
    } catch (e) {
        console.error(e)
        res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
    }
}

authorizedAdminRoutes.get("/users", getUsers)
authorizedAdminRoutes.put("/users", editUser)
authorizedAdminRoutes.get("/polls", getPolls)
authorizedAdminRoutes.delete("/user", deleteUser)

// eslint-disable-next-line new-cap
const adminRoutes = express.Router()

adminRoutes.use("/", checkLoggedIn, checkAdmin, authorizedAdminRoutes)

export default adminRoutes
