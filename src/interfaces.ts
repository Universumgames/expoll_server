/* eslint-disable no-unused-vars */
export type tUserID = number
export type tPollID = string
export type tOptionId = number

export type tDate = Date
export type tDateTime = Date

export enum ReturnCode {
    OK = 200,
    BAD_REQUEST = 400,
    MISSING_PARAMS = 400,
    INVALID_PARAMS = 400,
    UNAUTHORIZED = 401,
    INVALID_LOGIN_KEY = 401,
    NOT_ACCEPTABLE = 406,
    CONFLICT = 409,
    INVALID_TYPE = 409,
    INTERNAL_SERVER_ERROR = 500
}

/**
 * User object storing all user data
 */
export interface IUser {
    id: tUserID
    username: string
    firstName: string
    lastName: string
    mail: string
    polls: IPoll[]
    votes: IVote[]
    loginKey: string
    admin: boolean
}

export enum PollType {
    String = 0,
    Date = 1,
    DateTime = 2
}

/**
 * Poll meta data, except vote options and votes
 */
export interface IPoll {
    admin: IUser
    id: tPollID
    name: string
    created: Date
    updated: Date
    description: string
    type: PollType
    votes: IVote[]

    maxPerUserVoteCount: number
}

/**
 * Base class for a poll option to vote for
 */
export interface IPollOption {
    poll: IPoll
    id: tOptionId
}

/**
 * Poll option for a poll with strings
 */
export interface IPollOptionString extends IPollOption {
    value: string
}

/**
 * Poll Option with date (end optional)
 */
export interface IPollOptionDate extends IPollOption {
    dateStart: Date
    dateEnd: Date
}

/**
 * Poll option wht datetime (end optional)
 */
export interface IPollOptionDateTime extends IPollOption {
    dateTimeStart: Date
    dateTimeEnd: Date
}

/**
 * Vote object
 */
export interface IVote {
    id: number
    user: IUser
    poll: IPoll
    optionID: tOptionId
    votedFor: boolean
}
