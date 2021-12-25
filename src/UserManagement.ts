import { Request } from "express"
import Database from "./database"
import { Poll, Session, User, Vote } from "./entities/entities"
import { ReturnCode, tPollID, tUserID } from "./interfaces"
import { config } from "./config"
import getMailManager, { Mail } from "./MailManager"
import getPollManager from "./PollManagement"

/**
 * Manger class to create delete and change users
 */
class UserManager {
    private db: Database

    /**
     * create new User manager
     * @param {Database} db database object for database access
     */
    constructor(db: Database) {
        this.db = db
    }

    /**
     * Get typeorm Database user repository
     */
    private get repo() {
        return this.db.connection.getRepository(User)
    }

    /**
     * Create new User and store in database, if user exists, existing user is updated
     * @param {string} firstName first name of user
     * @param {string} lastName last name of user
     * @param {string} mail valid mail address of user
     * @param {string} username username for user
     * @return {User} returns new User object or already existent one
     */
    async createUser(firstName: string, lastName: string, mail: string, username: string): Promise<User | undefined> {
        const u = new User()
        u.mail = mail
        if ((await this.checkUserExists({ mail: mail })) || (await this.checkUserExists({ username: username })))
            return undefined
        u.username = username
        u.firstName = firstName
        u.lastName = lastName
        u.active = true

        console.log(u)

        u.save()

        return u
    }

    /**
     * Update a users values
     * @param {string} mail the users mail address
     * @param {any} settings values of the user to be changed
     * @return {User | undefined} returns edited User or undefined if it doe not exist
     */
    async updateUser(
        mail: string,
        settings: { firstName?: string; lastName?: string; username?: string }
    ): Promise<User | undefined> {
        const user = await this.getUser({ mail: mail })
        if (user == undefined) return undefined

        if (settings.firstName != undefined) user.firstName = settings.firstName
        if (settings.lastName != undefined) user.lastName = settings.lastName
        if (settings.username != undefined) user.username = settings.username
        user.save()

        return user
    }

    /**
     * Get User with given mail address
     * @param {{}} data the mail address of the user to be searched for
     * @param {string?} loginKey the loginKey of the user to be searched for
     * @return {Promise<User | undefined>} returns found User or undefined if not existant
     */
    async getUser(data: {
        mail?: string
        loginKey?: string
        username?: string
        userID?: tUserID
    }): Promise<User | undefined> {
        if (data.mail != undefined) return await this.repo.findOne({ where: { mail: data.mail } })
        else if (data.loginKey != undefined) {
            const session = await this.getSession(data.loginKey)
            if (session == undefined || !session.isValid) return undefined
            else return session.user
        } else if (data.username != undefined)
            return await this.repo.findOne({
                where: { username: data.username },
                relations: ["polls", "votes", "polls.admin"]
            })
        else if (data.userID != undefined)
            return await this.repo.findOne({
                where: { id: data.userID },
                relations: ["polls", "votes", "polls.admin"]
            })
        else return undefined
    }

    /**
     * Get session by loginKey
     * @param {string} loginKey the login key
     * @return {Session} return usersession
     */
    async getSession(loginKey: string): Promise<Session | undefined> {
        return await Session.findOne({
            where: { loginKey: loginKey },
            relations: ["user", "user.polls", "user.votes", "user.polls.admin"]
        })
    }

    /**
     * Check if user with mail address exists
     * @param {{string, string}} option the mail address of the user to search
     * @return {Promise<Boolean>} true when User exist, false otherwise
     */
    async checkUserExists(option: { mail?: string; username?: string }): Promise<Boolean> {
        const mailUser = option.mail != undefined ? await this.getUser({ mail: option.mail }) : undefined
        const nameUser = option.username != undefined ? await this.getUser({ username: option.username }) : undefined
        return mailUser != undefined || nameUser != undefined
    }

    /**
     * Check if user with loginKey exists
     * @param {string} loginKey the loginKey of the user to search
     * @return {Promise<Boolean>} true when User exist, false otherwise
     */
    async checkLoginKeyExists(loginKey: string): Promise<Boolean> {
        const u = await this.getUser({ loginKey: loginKey })
        return u != undefined
    }

    /**
     * delete an user account, or rather deactivate it, so that polls are still usable
     * @param {string} mail the to-be-deleted user's mail adress
     */
    async deactivateUser(mail: string) {
        const user = await this.getUser({ mail: mail })
        if (user != undefined) {
            user.active = false
            user.save()
        }
    }

    /**
     * Get Polls the user is "connected" to
     * @param {string} mail the users mail address
     * @return {Poll[]} users polls
     */
    async getPolls(mail: string): Promise<Poll[]> {
        const user = await this.getUser({ mail: mail })
        if (user == undefined) return []
        return user.polls
    }

    /**
     * Add poll to user access list
     * @param {string} mail users mail address
     * @param {tPollID} pollID the poll you want to add
     * @return {Promise<ReturnCode>} the returncode wether the process was successful or not
     */
    async addPoll(mail: string, pollID: tPollID): Promise<ReturnCode> {
        const user = await this.getUser({ mail: mail })
        if (user == undefined) return ReturnCode.INVALID_PARAMS
        const poll = await getPollManager().getPoll(pollID)
        if (poll == undefined) return ReturnCode.INVALID_PARAMS
        if (user.polls == undefined) user.polls = []
        user.polls.push(poll)
        await user.save()
        return ReturnCode.OK
    }

    /**
     * Get Votes from a User for s specific Poll
     * @param {string} mail user mail address
     * @param {tPollID} pollID the poll referring to
     * @return {Vote[]} given Votes from user to Poll
     */
    async getVotes(mail: string, pollID: tPollID): Promise<Vote[]> {
        const user = await this.getUser({ mail: mail })
        if (user == undefined) return []
        const votes: Vote[] = []
        user.votes.forEach((vote) => {
            if (vote.poll.id == pollID) {
                votes.push(vote)
            }
        })
        return votes
    }

    /**
     * send a mail to the user to receive a login link
     * @param {string} mail users mail address
     * @param {Request} req the server hostname
     * @return {ReturnCode} the returncode of the operation
     */
    async sendLoginMail(mail: string, req: Request): Promise<ReturnCode> {
        if (mail == undefined) return ReturnCode.MISSING_PARAMS
        const user = await this.getUser({ mail: mail })
        if (user == undefined) return ReturnCode.INVALID_PARAMS
        const key = await user.generateSession()
        getMailManager().sendMail({
            from: config.mailUser,
            to: user.mail,
            subject: "Loginkey for loggin into expoll",
            text:
                "Here is you login key for logging in on the expoll website: " +
                key.loginKey +
                "\n alternatively you can click this link " +
                urlBuilder(req, key.loginKey)
        } as Mail)
        return ReturnCode.OK
    }
}

/**
 * Build login url sent vie mail
 * @param {Request} req express request object
 * @param {string} loginKey the users login key
 * @return {string} the login url
 */
function urlBuilder(req: Request, loginKey: string): string {
    const port = req.app.settings.port || config.frontEndPort
    return (
        req.protocol +
        "://" +
        req.hostname +
        (port == 80 || port == 443 ? "" : ":" + port) +
        "/#/login/" +
        encodeURIComponent(loginKey)
    )
}

let userManager!: UserManager

/**
 * creates if not already present a new UserManager, if existent returns existing one
 * @param {Database} db Database object to grant data access
 * @return {UserManager} User Manager to manage all User todos
 */
export function createUserManager(db: Database): UserManager {
    if (userManager == undefined) userManager = new UserManager(db)
    return userManager
}

/**
 * returns initialized user manager
 * @return {UserManager} current user manager
 */
export default function getUserManager(): UserManager {
    return userManager
}
