import { Session } from "entities/session"
import { Buffer } from "buffer"
import { IUser, tPollID } from "expoll-lib/interfaces"
import { CookieOptions, Request } from "express"
import { config } from "./expoll_config"
import { MailRegexEntry } from "expoll-lib"

export const cookieName = "expoll_dat"

/**
 * create cookie config with expiration date
 * @param {ISession} session the user session
 * @return {CookieConfig} necessary cookie config for express
 */
export function cookieConfig(session: Session): CookieOptions {
    return { httpOnly: true, sameSite: "strict", expires: session.expiration }
}

/**
 * Get users login Key from cookie or from request body
 * @param {express.Request} req the express request process
 * @return {string} the login key
 */
export function getLoginKey(req: Request): string | undefined {
    if (req.cookies != undefined) {
        const cookie = req.cookies[cookieName]
        if (cookie != undefined) {
            const key = cookie.loginKey as string
            if (key != undefined) return key
        }
    }
    if (req.body != undefined) {
        if (req.body.loginKey != undefined) return req.body.loginKey as string
    }
    if (req.query != undefined) return req.query.loginKey as string
    return undefined
}

/**
 * Get users request for webauthn from cookie or from request body
 * @param {express.Request} req the express request process
 * @return {string} the login key
 */
export function getUserRequest(req: Request): { mail?: string; username?: string } {
    if (req.body != undefined) {
        if (req.body.username != undefined) return { username: req.body.username as string }
        if (req.body.mail != undefined) return { mail: req.body.mail as string }
    }
    if (req.query != undefined && req.query.username != undefined) return { username: req.query.username as string }
    if (req.query != undefined && req.query.mail != undefined) return { mail: req.query.mail as string }
    return {}
}

/**
 * Check if an user is any kind of admin
 * @param {IUser} user the user to check
 * @return {boolean} returns true if user is admin or super admin, false otherwise
 */
export function isAdmin(user: IUser): boolean {
    return user.admin || isSuperAdmin(user)
}

/**
 * Check if user is superadmin
 * @param {IUser} user the user to check
 * @return {boolean} return true if user is super admin (config file), false otherwise
 */
export function isSuperAdmin(user: IUser): boolean {
    return config.superAdminMail == user.mail
}

/**
 * @param {String} currentMetrics current metrics data
 * @param {String} key key of metric
 * @param {String} name name of metric
 * @param {String} duration duration of measured component
 * @return {String} Server_timings header string
 */
export function addServerTimingsMetrics(currentMetrics: string, key: string, name: string, duration: number): string {
    return currentMetrics + key + ';desc="' + name + '";dur=' + duration + ","
}

/**
 *
 * @param {Buffer} buf the buffer
 * @return {ArrayBuffer}
 */
export function toArrayBuffer(buf: any): ArrayBuffer {
    const ab = new ArrayBuffer(buf.length)
    const view = new Uint8Array(ab)
    for (let i = 0; i < buf.length; ++i) {
        view[i] = buf[i]
    }
    return ab
}

/**
 *
 * @param {ArrayBuffer} ab the Buffer
 * @return {Buffer}
 */
export function toBuffer(ab: ArrayBuffer): Buffer {
    const buf = Buffer.alloc(ab.byteLength)
    const view = new Uint8Array(ab)
    for (let i = 0; i < buf.length; ++i) {
        buf[i] = view[i]
    }
    return buf
}

/**
 *
 * @param {any} buffer the buffer
 * @return {string}
 */
export function bufferToBase64URLString(buffer: any): string {
    const bytes = new Uint8Array(buffer)
    let str = ""
    for (const charCode of bytes) {
        str += String.fromCharCode(charCode)
    }
    const base64String = btoa(str)
    return base64String.replace(/\+/g, "-").replace(/\//g, "_").replace(/=/g, "")
}

/**
 *
 * @param {string} base64URLString
 * @return {Buffer}
 */
export function base64URLStringToBuffer(base64URLString: string) {
    const base64 = base64URLString.replace(/-/g, "+").replace(/_/g, "/")
    const padLength = (4 - (base64.length % 4)) % 4
    const padded = base64.padEnd(base64.length + padLength, "=")
    const binary = atob(padded)
    const buffer = new ArrayBuffer(binary.length)
    const bytes = new Uint8Array(buffer)
    for (let i = 0; i < binary.length; i++) {
        bytes[i] = binary.charCodeAt(i)
    }
    return buffer
}

/**
 * Check if a mail is not banned
 * @param {String} mail the mial adress to check
 * @param {MailRegexEntry[]} regexRules not allowed mail adresss
 * @return {boolean} returns true if mail is allowed, false otherwise
 */
export function mailIsAllowed(mail: string, regexRules: MailRegexEntry[]): boolean {
    let res = true
    for (const regex of regexRules) {
        if ((mail.match(regex.regex) && regex.blacklist) || (!mail.match(regex.regex) && !regex.blacklist)) {
            res = false
        }
    }
    return res
}

/**
 * generates a url the user can click on to join the poll
 * @param {tPollID} pollID the pollid to share
 * @return {string} the url to share the poll
 */
export function generateShareURL(pollID: tPollID): string {
    return config.shareURLPrefix + pollID
}
