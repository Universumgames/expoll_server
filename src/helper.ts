import { Request } from "express"

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

    return undefined
}
