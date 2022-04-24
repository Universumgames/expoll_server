import { IUser } from "expoll-lib/interfaces"
import { Request } from "express"
import { config } from "./expoll_config"

export const cookieName = "expoll_dat"

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
