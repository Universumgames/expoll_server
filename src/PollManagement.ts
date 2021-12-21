import Database from "./database"
import { Poll, PollOptionDate, PollOptionDateTime, PollOptionString, User, Vote } from "./entities/entities"
import { PollType, tDate, tDateTime, tOptionId, tPollID, tUserID } from "./interfaces"
import getUserManager from "./UserManagement"

type basicPollOptions = { admin: User; name: string; description: string }
/**
 * Manager class for polls
 */
class PollManager {
    private db: Database

    /**
     * create new Poll manager
     * @param {Database} db database object for database access
     */
    constructor(db: Database) {
        this.db = db
    }

    /**
     * Get typeorm Database poll repository
     */
    private get repo() {
        return this.db.connection.getRepository(Poll)
    }

    /**
     * Get Poll by pollid
     * @param {tPollID} pollID the id of the poll
     * @return {Poll | undefined} returns corresponding poll or undefined if not found
     */
    async getPoll(pollID: tPollID): Promise<Poll | undefined> {
        return this.db.connection.getRepository(Poll).findOne({ where: { id: pollID } })
    }

    // #region string polls
    /**
     * Creates new Poll with strings as select options
     * @param {basicPollOptions} settings basic poll settings like name, admin and a description
     * @param {string[]} options the options the user can choose from
     */
    async createStringPoll(settings: basicPollOptions, options: string[]): Promise<Poll> {
        const poll = new Poll()
        poll.type = PollType.String
        poll.admin = settings.admin
        poll.description = settings.description
        poll.name = settings.name
        // poll.created = new Date()
        // poll.updated = new Date()

        options.forEach((option) => {
            const voteOption = new PollOptionString()
            voteOption.poll = poll
            voteOption.value = option
            voteOption.save()
        })

        poll.save()
        return poll
    }

    /**
     * Get the polling options for given poll
     * @param {tPollID} pollID the poll id from which the options should be received
     * @return {PollOptionString[] | undefined} return array of options or undefined \
     * if polltype not corresponds to string or poll does not exist
     */
    async getStringPollOptions(pollID: tPollID): Promise<PollOptionString[] | undefined> {
        const poll = await this.repo.findOne({ where: { id: pollID } })
        if (poll == undefined || poll.type != PollType.String) return undefined
        const options: PollOptionString[] = await this.db.connection
            .getRepository(PollOptionString)
            .find({ where: { poll: poll } })
        return options
    }

    // #endregion

    // #region date polls
    /**
     * Creates new Poll with dates as select options
     * @param {basicPollOptions} settings basic poll settings like name, admin and a description
     * @param {{start:tDate, end: tDate}[]} options the options the user can choose from
     */
    async createDatePoll(settings: basicPollOptions, options: { start: tDate; end: tDate }[]): Promise<Poll> {
        const poll = new Poll()
        poll.type = PollType.Date
        poll.admin = settings.admin
        poll.description = settings.description
        poll.name = settings.name
        // poll.created = new Date()
        // poll.updated = new Date()

        options.forEach((option) => {
            const voteOption = new PollOptionDate()
            voteOption.poll = poll
            voteOption.dateStart = option.start
            voteOption.dateEnd = option.end
            voteOption.save()
        })

        poll.save()
        return poll
    }

    /**
     * Get the polling options for given poll
     * @param {tPollID} pollID the poll id from which the options should be received
     * @return {PollOptionDate[] | undefined} return array of options or undefined \
     * if polltype not corresponds to string or poll does not exist
     */
    async getDatePollOptions(pollID: tPollID): Promise<PollOptionDate[] | undefined> {
        const poll = await this.repo.findOne({ where: { id: pollID } })
        if (poll == undefined || poll.type != PollType.Date) return undefined
        const options: PollOptionDate[] = await this.db.connection
            .getRepository(PollOptionDate)
            .find({ where: { poll: poll } })
        return options
    }
    // #endregion

    // #region date time polls
    /**
     * Creates new Poll with datetime as select options
     * @param {basicPollOptions} settings basic poll settings like name, admin and a description
     * @param {{start:tDateTime, end: tDateTime}[]} options the options the user can choose from
     */
    async createDateTimePoll(
        settings: basicPollOptions,
        options: { start: tDateTime; end: tDateTime }[]
    ): Promise<Poll> {
        const poll = new Poll()
        poll.type = PollType.DateTime
        poll.admin = settings.admin
        poll.description = settings.description
        poll.name = settings.name
        // poll.created = new Date()
        // poll.updated = new Date()

        options.forEach((option) => {
            const voteOption = new PollOptionDateTime()
            voteOption.poll = poll
            voteOption.dateTimeStart = option.start
            voteOption.dateTimeEnd = option.end
            voteOption.save()
        })

        poll.save()
        return poll
    }

    /**
     * Get the polling options for given poll
     * @param {tPollID} pollID the poll id from which the options should be received
     * @return {PollOptionDateTime[] | undefined} return array of options or undefined \
     * if polltype not corresponds to string or poll does not exist
     */
    async getDateTimePollOptions(pollID: tPollID): Promise<PollOptionDateTime[] | undefined> {
        const poll = await this.repo.findOne({ where: { id: pollID } })
        if (poll == undefined || poll.type != PollType.DateTime) return undefined
        const options: PollOptionDateTime[] = await this.db.connection
            .getRepository(PollOptionDateTime)
            .find({ where: { poll: poll } })
        return options
    }
    // #endregion

    /**
     * Get users that voted in a poll
     * @param {tPollID} pollID the id of the poll
     * @return {User[]} array of users voted for poll
     */
    async getContributedUsers(pollID: tPollID): Promise<User[]> {
        const poll = await this.getPoll(pollID)
        if (poll == undefined) return []
        const users: User[] = []
        poll.votes.forEach((vote) => {
            if (!users.includes(vote.user)) users.push(vote.user)
        })
        return users
    }

    /**
     * g
     * @param {tUserID} userID the id of the user to check
     * @param {tPollID} pollID the poll id
     * @return {number} returns the number of votes the user already has on that poll
     */
    async getVoteCountFromUser(userID: tUserID, pollID: tPollID): Promise<number> {
        const user = await getUserManager().getUser({ userID: userID })
        if (user == undefined) return -1
        const poll = await this.getPoll(pollID)
        if (poll == undefined) return -1
        if (poll.votes == undefined) return 0
        const votes = poll.votes.filter((element) => element.poll.id == poll.id && element.user.id == user.id)
        return votes.length
    }

    /**
     * Get existing vote on option
     * @param {tUserID} userID user id
     * @param {tPollID} pollID poll id
     * @param {tOptionID} optionID selected option
     * @return {Promise<Vote | undefined>} returns existing Vote or undefined if not existent
     */
    async getVote(userID: tUserID, pollID: tPollID, optionID: tOptionId): Promise<Vote | undefined> {
        const user = await getUserManager().getUser({ userID: userID })
        if (user == undefined) return undefined
        if (user.votes == undefined) return undefined
        return user.votes.find((element) => element.optionID == optionID && element.poll.id == pollID)
    }
}

let pollManger!: PollManager

/**
 * creates if not already present a new UserManager, if existent returns existing one
 * @param {Database} db Database object to grant data access
 * @return {PollManger} Poll Manager to manage all Poll tasks
 */
export function createPollManager(db: Database): PollManager {
    if (pollManger == undefined) pollManger = new PollManager(db)
    return pollManger
}

/**
 * returns initiliazed poll manager
 * @return {PollManager} current pollmanager
 */
export default function getPollManager(): PollManager {
    return pollManger
}
