/* eslint-disable new-cap */
import {
    Entity,
    Column,
    PrimaryGeneratedColumn,
    BaseEntity,
    PrimaryColumn,
    ManyToOne,
    ManyToMany,
    SaveOptions,
    Repository,
} from "typeorm"

type tUserID = number
type tPollID = number
type tOptionId = number

@Entity()
/**
 * User object storing all user data
 */
export class User extends BaseEntity {
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
}

@Entity()
/**
 * A quick login with an URL
 */
export class quickLogin extends BaseEntity {
    @ManyToOne((type) => User, (user) => user.id)
    user: User

    @PrimaryColumn()
    link: string
}

@Entity()
/**
 * Poll meta data, except vote options and votes
 */
export class Poll extends BaseEntity {
    @ManyToOne((type) => User, (user) => user.id)
    admin: User

    @PrimaryGeneratedColumn()
    id: tPollID

    @Column()
    name: string

    @Column()
    created: Date

    @Column()
    updated: Date

    @Column()
    description: string

    @Column()
    polltype: PollType

    @ManyToOne((type) => Vote, (vote) => vote.id)
    votes: Vote[]
}

enum PollType {
    String = 0,
    Date = 1,
    DateTime = 2,
}

/**
 * Base class for a poll option to vote for
 */
export abstract class PollOption extends BaseEntity {
    @ManyToOne((type) => Poll, (poll) => poll.id)
    poll: Poll

    @PrimaryColumn()
    id: tOptionId
}

@Entity()
/**
 * Poll option for a poll with strings
 */
export class pollOptionString extends PollOption {
    @Column()
    value: string
}

@Entity()
/**
 * Poll Option with date (end optional)
 */
export class pollOptionDate extends PollOption {
    @Column()
    dateStart: Date

    @Column({ nullable: true })
    dateEnd!: Date
}

@Entity()
/**
 * Poll option wht datetime (end optional)
 */
export class pollOptionDateTime extends PollOption {
    @Column()
    dateTimeStart: Date

    @Column({ nullable: true })
    dateTimeEnd!: Date
}

@Entity()
/**
 * Vote object
 */
export class Vote extends BaseEntity {
    @PrimaryGeneratedColumn()
    id: number

    @ManyToMany((type) => User, (user) => user.id)
    user: User

    @ManyToMany((type) => Poll, (poll) => poll.id)
    poll: Poll

    optionID: tOptionId
}
