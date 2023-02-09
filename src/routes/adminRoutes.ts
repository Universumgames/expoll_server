import { UserInfo } from "expoll-lib/adminInterfaces"
import express, { NextFunction, Request, Response } from "express"
import { config } from "../expoll_config"
import { ReturnCode } from "expoll-lib/interfaces"
import getPollManager from "../PollManagement"
import getUserManager from "../UserManagement"
import { checkAdmin, checkLoggedIn } from "./routeHelper"
import { AdminPollListResponse, AdminUserListResponse, AdminEditUserRequest } from "expoll-lib/requestInterfaces"
import { SimplePoll } from "expoll-lib/extraInterfaces"
import { MailRegexRules, Session, User } from "./../entities/entities"
import {
    isSuperAdmin,
    addServerTimingsMetrics,
    cookieName,
    cookieConfig,
    getDataFromAny,
    mailIsAllowed
} from "../helper"
import getMailManager, { Mail } from "../MailManager"

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

        const t1 = new Date()
        const users = await getUserManager().getUsers()
        const t2 = new Date()

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
        const t3 = new Date()

        // @ts-ignore
        let metrics = req.metrics
        metrics = addServerTimingsMetrics(metrics, "userlist", "Retrieve Userlist from DB", t2.getTime() - t1.getTime())
        metrics = addServerTimingsMetrics(metrics, "simplifyUsers", "Simplify userlist", t3.getTime() - t2.getTime())

        const data: AdminUserListResponse = {
            users: cleanedUsers,
            totalCount: cleanedUsers.length
        }
        return res.set("Server-Timing", metrics).status(ReturnCode.OK).json(data)
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
                pollID: poll.id,
                editable: poll.allowsEditing
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

const mailRegexEdit = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const mailRegex = req.body.mailRegex as { regex: string; blacklist: boolean }[]
        if (mailRegex == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()

        // clear old mail regex
        await MailRegexRules.clear()
        // save new mail regex
        for (const regex of mailRegex) {
            const mailRegexEntity = new MailRegexRules()
            mailRegexEntity.regex = regex.regex
            mailRegexEntity.blacklist = regex.blacklist
            await mailRegexEntity.save()
        }

        res.status(ReturnCode.OK).end()
    } catch (e) {
        console.error(e)
        res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
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

const createUser = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const createUserReq = req.body
        const mail = createUserReq.mail
        const firstName = createUserReq.firstName
        const lastName = createUserReq.lastName
        const username = createUserReq.username

        if (mail == undefined || firstName == undefined || lastName == undefined || username == undefined)
            return res.status(ReturnCode.MISSING_PARAMS).end()

        // check user not exist
        if (
            (await getUserManager().checkUserExists({ mail: mail })) ||
            (await getUserManager().checkUserExists({ username: username }))
        )
            return res.status(ReturnCode.USER_EXISTS).end()

        if (!mailIsAllowed(mail, await MailRegexRules.find())) return res.status(ReturnCode.NOT_ACCEPTABLE).end()
        // create user
        const user = new User()
        user.mail = mail
        user.firstName = firstName
        user.lastName = lastName
        user.username = username
        try {
            await user.save()

            const port = req.app.settings.port || config.frontEndPort
            getMailManager().sendMail({
                from: config.mailUser,
                to: user.mail,
                subject: "Thank you for registering in expoll",
                text:
                    "Thank you for creating an account at over at expoll (" +
                    req.protocol +
                    "://" +
                    config.loginLinkURL +
                    (port == 80 || port == 443 ? "" : ":" + port) +
                    ")"
            } as Mail)

            return res.status(ReturnCode.OK).end()
        } catch (e) {
            console.error(e)
            return res.status(500).end()
        }
    } catch (e) {
        console.error(e)
        res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
    }
}

const impersonate = async (req: Request, res: Response, next: NextFunction) => {
    try {
        // @ts-ignore
        // const admin = req.user as User
        const impersonateID = req.body.impersonateID
        if (impersonateID == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()

        const user = await getUserManager().getUser({ userID: impersonateID })
        if (user == undefined) return res.status(ReturnCode.INVALID_PARAMS).end()
        if (isSuperAdmin(user)) return res.status(ReturnCode.UNAUTHORIZED).end()


        const session = await user.generateSession()

        const data = {
            loginKey: session.loginKey,
            // @ts-ignore
            originalLoginKey: req.loginKey
        }
        return res.status(ReturnCode.OK).cookie(cookieName, data, cookieConfig(session!)).json(user)
    } catch (e) {
        console.error(e)
        return res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
    }
}

const isImpersonating = async (req: Request, res: Response, next: NextFunction) => {
    try {
        // @ts-ignore
        const user = req.user as User
        // @ts-ignore
        const loginKey = getDataFromAny(req, "originalLoginKey")

        const session = await Session.findOne({ where: { loginKey: loginKey } })
        if (session == undefined) return res.status(ReturnCode.INVALID_PARAMS).end()

        return res.status(ReturnCode.OK).json(user)
    } catch (e) {
        console.error(e)
        return res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
    }
}


const unimpersonate = async (req: Request, res: Response, next: NextFunction) => {
    try {
        // @ts-ignore
        const admin = req.originalUser as User
        const originalLoginKey = getDataFromAny(req, "originalLoginKey")

        const session = await Session.findOne({ where: { loginKey: originalLoginKey } })
        if (session == undefined) return res.status(ReturnCode.INVALID_PARAMS).end()

        const data = {
            // @ts-ignore
            loginKey: originalLoginKey
        }
        return res.status(ReturnCode.OK).cookie(cookieName, data, cookieConfig(session!)).json(admin)
    } catch (e) {
        console.error(e)
        return res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
    }
}

authorizedAdminRoutes.get("/users", getUsers)
authorizedAdminRoutes.put("/users", editUser)
authorizedAdminRoutes.get("/polls", getPolls)
authorizedAdminRoutes.delete("/user", deleteUser)
authorizedAdminRoutes.post("/mailregex", mailRegexEdit)
authorizedAdminRoutes.get("/mailregex", mailRegexList)
authorizedAdminRoutes.post("/impersonate", impersonate)
authorizedAdminRoutes.post("/createUser", createUser)

// eslint-disable-next-line new-cap
const adminRoutes = express.Router()

adminRoutes.post("/unimpersonate", checkLoggedIn, unimpersonate)
adminRoutes.get("/isImpersonating", checkLoggedIn, isImpersonating)
adminRoutes.use("/", checkLoggedIn, checkAdmin, authorizedAdminRoutes)


export default adminRoutes
