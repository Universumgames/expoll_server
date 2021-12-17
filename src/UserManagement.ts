import Database from "./database"
import { Poll, User, Vote } from "./entities/entities"
import { tPollID } from "./interfaces"

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
    async createUser(firstName: string, lastName: string, mail: string, username: string): Promise<User> {
        const u = new User()
        u.mail = mail
        const existing = this.getUser(mail)
        if (existing != undefined) Object.assign(u, existing)
        u.username = username
        u.firstName = firstName
        u.lastName = lastName
        u.active = true

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
        const user = await this.getUser(mail)
        if (user == undefined) return undefined

        if (settings.firstName != undefined) user.firstName = settings.firstName
        if (settings.lastName != undefined) user.lastName = settings.lastName
        if (settings.username != undefined) user.username = settings.username
        user.save()

        return user
    }

    /**
     * Get User with given mail address
     * @param {string?} mail the mail address of the user to be searched for
     * @param {string?} loginKey the loginKey of the user to be searched for
     * @return {User | undefined} returns found User or undefined if not existant
     */
    async getUser(mail?: string, loginKey?: string): Promise<User | undefined> {
        if (mail != undefined) return await this.repo.findOne({ where: { mail: mail } })
        else return await this.repo.findOne({ where: { loginKey: loginKey } })
    }

    /**
     * Check if user with mail address exists
     * @param {string} mail the mail address of the user to search
     * @return {Promise<Boolean>} true when User exist, false otherwise
     */
    async checkUserExists(mail: string): Promise<Boolean> {
        return (await this.getUser(mail)) != undefined
    }

    /**
     * Check if user with loginKey exists
     * @param {string} loginKey the loginKey of the user to search
     * @return {Promise<Boolean>} true when User exist, false otherwise
     */
    async checkLoginKeyExists(loginKey: string): Promise<Boolean> {
        return (await this.getUser(undefined, loginKey)) != undefined
    }

    /**
     * delete an user account, or rather deactivate it, so that polls are still usable
     * @param {string} mail the to-be-deleted user's mail adress
     */
    async deactivateUser(mail: string) {
        const user = await this.getUser(mail)
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
        const user = await this.getUser(mail)
        if (user == undefined) return []
        return user.polls
    }

    /**
     * Get Votes from a User for s specific Poll
     * @param {string} mail user mail address
     * @param {tPollID} pollID the poll referring to
     * @return {Vote[]} given Votes from user to Poll
     */
    async getVotes(mail: string, pollID: tPollID): Promise<Vote[]> {
        const user = await this.getUser(mail)
        if (user == undefined) return []
        const votes: Vote[] = []
        user.votes.forEach((vote) => {
            if (vote.poll.id == pollID) {
                votes.push(vote)
            }
        })
        return votes
    }
}

let userManager!: UserManager

/**
 * creates if not already present a new UserManager, if existent returns existing one
 * @param {Database} db Database object to grant data access
 * @return {UserManager} User Manager to manage all User todos
 */
export default function createUserManager(db: Database): UserManager {
    if (userManager == undefined) userManager = new UserManager(db)
    return userManager
}
