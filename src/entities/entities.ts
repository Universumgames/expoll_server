import { IPoll, IPollOption, IPollOptionDate, IPollOptionDateTime, IUser, PollType } from "./../interfaces"
/* eslint-disable new-cap */
import { v4 as uuidv4 } from "uuid"
import {
    Entity,
    Column,
    PrimaryGeneratedColumn,
    BaseEntity,
    PrimaryColumn,
    ManyToOne,
    ManyToMany,
    CreateDateColumn,
    UpdateDateColumn,
    OneToMany
} from "typeorm"
import { tOptionId, tPollID, tUserID } from "../interfaces"

@Entity()
/**
 * User object storing all user data
 */
export class User extends BaseEntity implements IUser {
    @PrimaryGeneratedColumn()
    id: tUserID

    @Column({ unique: true })
    username: string

    @Column()
    firstName: string

    @Column()
    lastName: string

    @Column({ unique: true })
    mail: string

    @ManyToMany((type) => Poll, (poll) => poll.id)
    polls: Poll[]

    @ManyToMany((type) => Vote, (vote) => vote.id)
    votes: Vote[]

    @Column({ unique: true })
    loginKey: string = User.generateLoginKey()

    @Column({ default: true })
    active: boolean

    @Column({ default: false })
    admin: boolean

    /**
     * Generate new random login key
     * @return {String} new login key
     */
    private static generateLoginKey(): string {
        return uuidv4()
    }
}

@Entity()
/**
 * Poll meta data, except vote options and votes
 */
export class Poll extends BaseEntity implements IPoll {
    @ManyToOne((type) => User, (user) => user.id, { nullable: false })
    admin: User

    @PrimaryColumn()
    id: tPollID = Poll.generatePollID()

    /**
     * Generate new random poll id
     * @return {String} new poll id
     */
    private static generatePollID(): string {
        return uuidv4()
    }

    @Column()
    name: string

    @CreateDateColumn()
    created: Date

    @UpdateDateColumn()
    updated: Date

    @Column()
    description: string

    @Column()
    type: PollType = PollType.String

    @OneToMany((type) => Vote, (vote) => vote.id)
    votes: Vote[]

    @Column()
    /**
     * sets the number votes each user can send for this poll
     * use a number <= 0 to set it to infinity
     */
    maxPerUserVoteCount: number = -1
}

/**
 * Base class for a poll option to vote for
 */
export abstract class PollOption extends BaseEntity implements IPollOption {
    @ManyToOne((type) => Poll, (poll) => poll.id, { nullable: false })
    poll: Poll

    @PrimaryColumn()
    id: tOptionId
}

@Entity()
/**
 * Poll option for a poll with strings
 */
export class PollOptionString extends PollOption implements IPollOption {
    @Column()
    value: string
}

@Entity()
/**
 * Poll Option with date (end optional)
 */
export class PollOptionDate extends PollOption implements IPollOptionDate {
    @Column({ type: "date" })
    dateStart: Date

    @Column({ nullable: true, type: "date" })
    dateEnd!: Date
}

@Entity()
/**
 * Poll option wht datetime (end optional)
 */
export class PollOptionDateTime extends PollOption implements IPollOptionDateTime {
    @Column({ type: "datetime" })
    dateTimeStart: Date

    @Column({ nullable: true, type: "datetime" })
    dateTimeEnd!: Date
}

@Entity()
/**
 * Vote object
 */
export class Vote extends BaseEntity {
    @PrimaryGeneratedColumn()
    id: number

    @ManyToOne((type) => User, (user) => user.id, { nullable: false })
    user: User

    @ManyToOne((type) => Poll, (poll) => poll.id, { nullable: false })
    poll: Poll

    @Column()
    optionID: tOptionId

    @Column()
    votedFor: boolean = false
}
