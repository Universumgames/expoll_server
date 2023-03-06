import getMailManager, { Mail } from "./../MailManager"
import { config } from "../expoll_config"
import axios from "axios"
import { checkLoggedIn } from "./routeHelper"
import { addServerTimingsMetrics, base64URLStringToBuffer, getDataFromAny, mailIsAllowed } from "./../helper"
import { ReturnCode } from "expoll-lib/interfaces"
import { DeleteConfirmation, MailRegexRules, User } from "./../entities/entities"
import express, { NextFunction, Request, Response } from "express"
import getUserManager from "../UserManagement"
import { CreateUserRequest, CreateUserResponse } from "expoll-lib/requestInterfaces"
import { SimplePoll } from "expoll-lib"
import getPollManager from "../PollManagement"
import * as cbor from "cbor"
import { IAppleAppAttest } from "../entities/appAttest"
import * as crypto from "crypto"
import * as fs from "fs"


// eslint-disable-next-line new-cap
const userRoutes = express.Router()

/* const test = async (req: Request, res: Response, next: NextFunction) => {
    return res.status(200).json({
        message: "Test successful"
    })
} */

/**
 * Verify received captcha token
 * @param {string} token the token received by the client
 * @return {boolean} true if token is valid false otherwise
 */
async function verifyCaptcha(token: string): Promise<boolean> {
    const googleReturn = (
        await axios.post("https://www.google.com/recaptcha/api/siteverify", undefined, {
            params: {
                secret: config.recaptchaAPIKey,
                response: token
            }
        })
    ).data
    return googleReturn.score >= 0.5 ?? false
}

/**
 * Verify if request is from a legit apple device
 * @param {String} attest the attestation
 * @param {String} challengeId the challenge id
 * @return {boolean} true if request is legit false otherwise
 * @see https://developer.apple.com/documentation/devicecheck
 */
async function verifyAppleAppAttest(attest: string, challengeId: string): Promise<boolean> {
    const buffer = base64URLStringToBuffer(attest)
    const decoded = await cbor.decodeFirst(buffer) as IAppleAppAttest
    /* const stored = await AppAttests.findOne({ where: { uuid: challengeId } })
    console.log("decoded", decoded)
    console.log("stored", stored)
    if (stored == undefined) return false*/

    // load keys
    const credCert = new crypto.X509Certificate(decoded.attStmt.x5c[0])
    const caCert = new crypto.X509Certificate(decoded.attStmt.x5c[1])
    const rootCert = new crypto.X509Certificate(fs.readFileSync("config/Apple_App_Attestation_Root_CA.pem"))
    // 1. verify certificate chain
    const first = credCert.verify(caCert.publicKey)
    const second = caCert.verify(rootCert.publicKey)
    // console.log(first, second)
    if (!first || !second) return false

    // i have verified that the request is from a legit apple device
    return true

    // continue with verification to test for legit app
    // see https://blog.restlesslabs.com/john/ios-app-attest
    // see https://developer.apple.com/documentation/devicecheck/validating_apps_that_connect_to_your_server
    /*
    // 2. create own hash and concat with authData
    const clientDataHash = crypto.createHash("sha256").update(stored.challenge).digest()
    const composite = Buffer.concat([decoded.authData, clientDataHash])
    // console.log("composite", composite)

    // 3. create nonce
    const nonce = crypto.createHash("sha256").update(composite).digest()
    // console.log("nonce", nonce)

    // 4. obtain value of credCert extension with OID 1.2.840.113635.100.8.2, decode sequence and test against nonce
    const decodeASN1 = asn1.fromBER(credCert.raw).result.valueBlock
    // find extension with OID 1.2.840.113635.100.8.2
    const oidValue = (decodeASN1 as any).value[0]
    // console.log("decodeASN1", decodeASN1)
    // console.log("oid_value", oidValue)


    // decode credCert with der encoding asn.1 sequence


    return true*/
}

const createUserChallenge = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const username = getDataFromAny(req, "username") as string
        const mail = getDataFromAny(req, "mail") as string
        return res.status(ReturnCode.OK).send("challenge" + username + mail)
    } catch (e) {
        return res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
    }
}

/* userRoutes.post("/test", async (req: Request, res: Response, next: NextFunction) => {
    await verifyAppleAppAttest(req.body.attest, "1")
    return res.status(200).end()
}) */

const createUser = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const t1 = new Date()
        // check valid request body
        if (req.body == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
        const body = req.body as CreateUserRequest
        const mail = body.mail as string
        if (mail == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
        const firstName = body.firstName as string
        if (firstName == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
        const lastName = body.lastName as string
        if (lastName == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
        const username = body.username as string
        if (username == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
        if (body.captcha == undefined && body.appAttest == undefined) return res.status(ReturnCode.MISSING_PARAMS).end()
        const captchaToken = body.captcha
        const appAttest = body.appAttest

        const t2 = new Date()

        if ((captchaToken != undefined && !(await verifyCaptcha(captchaToken))) ||
            (appAttest != undefined && !(await verifyAppleAppAttest(appAttest, "")))) {
            return res.status(ReturnCode.CAPTCHA_INVALID).end()
        }
        // TODO retrieve challenge id from request

        const t3 = new Date()

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
            const t4 = new Date()
            await user.save()

            const t5 = new Date()
            const session = await user.generateSession()
            const loginKey = session.loginKey

            const data: CreateUserResponse = {
                loginKey: loginKey
            }

            const port = req.app.settings.port || config.frontEndPort
            getMailManager().sendMail({
                from: config.mail.mailUser,
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

            const t6 = new Date()

            // @ts-ignore
            let metrics = req.metrics
            metrics = addServerTimingsMetrics(
                metrics,
                "paramsCheck",
                "Check Required Parameters for user creation",
                t2.getTime() - t1.getTime()
            )
            metrics = addServerTimingsMetrics(metrics, "captcha", "Verify captcha", t3.getTime() - t2.getTime())
            metrics = addServerTimingsMetrics(
                metrics,
                "checkExisting",
                "Check if Mail and username is unique",
                t4.getTime() - t3.getTime()
            )
            metrics = addServerTimingsMetrics(metrics, "saveUser", "Create User in DB", t5.getTime() - t4.getTime())
            metrics = addServerTimingsMetrics(metrics, "mailSend", "Send mail to user", t6.getTime() - t5.getTime())

            return (
                res
                    // @ts-ignore
                    .set("Server-Timing", metrics)
                    .status(ReturnCode.OK)
                    .json(data)
            )
        } catch (e) {
            console.error(e)
            return res.status(500).end()
        }
    } catch (e) {
        console.error(e)
        res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
    }
}

const getUserData = async (req: Request, res: Response, next: NextFunction) => {
    try {
        // @ts-ignore
        const user = req.user as User
        // @ts-ignore
        const loginKey = req.loginKey as string

        const t1 = new Date()
        const easyUser = user
        const cookieSessionReq = getUserManager().getSession(loginKey)

        easyUser.admin = getUserManager().userIsAdminOrSuperAdminSync(user)

        const data = {
            loginKey: loginKey
        }
        const session = await cookieSessionReq
        const t2 = new Date()

        return (
            res
                // @ts-ignore
                .set(
                    "Server-Timing",
                    addServerTimingsMetrics(
                        // @ts-ignore
                        req.metrics,
                        "cookieSet",
                        "Transform userdata and set cookie",
                        t2.getTime() - t1.getTime()
                    )
                )
                .status(ReturnCode.OK)
                .json(easyUser)
        )
    } catch (e) {
        console.error(e)
        res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
    }
}

const deleteUser = async (req: Request, res: Response, next: NextFunction) => {
    try {
        // @ts-ignore
        const user = req.user as User

        const conf = new DeleteConfirmation()
        conf.user = user
        conf.expiration = new Date()
        conf.expiration.setHours(conf.expiration.getHours() + 1)
        await conf.save()
        getMailManager().sendMail({
            from: config.mail.mailUser,
            to: user.mail,
            subject: "Delete Account",
            text:
                "Please click on the following link to delete your account, " +
                "the link is valid for an hour, once the link is opened, the action cannot be undone: \n" +
                deleteUserConfirmURLBuilder(req, conf.id)
        })
        return res.status(ReturnCode.USER_EXISTS).json({ message: "Confirmation email sent" })
    } catch (e) {
        console.error(e)
        res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
    }
}

const deleteUserConfirm = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const id = req.params.id

        const conf = await DeleteConfirmation.findOne({ id: id }, { relations: ["user"] })
        if (conf == undefined) return res.status(ReturnCode.BAD_REQUEST).end()
        if (conf.expiration.getTime() < Date.now()) {
            await conf.remove()
            return res.status(ReturnCode.BAD_REQUEST).json({ message: "Confirmation expired" })
        }

        const deleteRes = await getUserManager().deleteUser(conf.user.id)

        if (deleteRes == ReturnCode.OK) return res.status(deleteRes).send("User deleted").redirect("/")
        res.status(deleteRes).end()
    } catch (e) {
        console.error(e)
        res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
    }
}

/**
 * Create url for deleting a user
 * @param {Request} req express Request
 * @param {String} confirmKey deletion key
 * @return {String} the url the user has to click to confirm the deletion
 */
function deleteUserConfirmURLBuilder(req: Request, confirmKey: string) {
    const port = req.app.settings.port || config.frontEndPort
    return (
        req.protocol +
        "://" +
        config.loginLinkURL +
        (port == 80 || port == 443 ? "" : ":" + port) +
        "/confirm/delete?id=" +
        encodeURIComponent(confirmKey)
    )
}

const getPersonalizedData = async (req: Request, res: Response, next: NextFunction) => {
    try {
        // @ts-ignore
        const user = req.user as User

        const t1 = new Date()
        let polls: SimplePoll[] = []
        const pollQueue: Promise<SimplePoll | undefined>[] = []
        for (const poll of user.polls) {
            pollQueue.push(getPollManager().getSimplePoll(poll.id))
        }

        // @ts-ignore
        // const activeSession = (await getUserManager().getSession(req.loginKey)) as Session
        const sessions = (await getUserManager().getSessions(user.id)).map((session) => {
            if (!session) return undefined
            // @ts-ignore
            if (session.loginKey == req.loginKey)
                return {
                    expiration: session.expiration,
                    userAgent: session.userAgent,
                    shortKey: session.loginKey.substring(0, 4),
                    active: true
                }
            return {
                expiration: session.expiration,
                userAgent: session.userAgent,
                shortKey: session.loginKey.substring(0, 4)
            }
        })
        const t2 = new Date()

        for (const pollWait of pollQueue) {
            const poll = await pollWait
            if (poll != undefined) {
                polls.push(poll)
            }
        }
        polls = polls.sort((ele2, ele1) => ele1.lastUpdated.getTime() - ele2.lastUpdated.getTime())

        const simpleUser = {
            id: user.id,
            username: user.username,
            firstName: user.firstName,
            lastName: user.lastName,
            mail: user.mail,
            admin: getUserManager().userIsAdminOrSuperAdminSync(user),
            superAdmin: getUserManager().userIsSuperAdminSync(user),
            authenticators: user.authenticators,
            polls: polls,
            sessions: sessions,
            votes: user.votes
        }
        // @ts-ignore
        let metrics = req.metrics
        metrics = addServerTimingsMetrics(metrics, "sessionlist", "Get sessions summary", t2.getTime() - t1.getTime())
        metrics = addServerTimingsMetrics(
            metrics,
            "polllist",
            "Get poll summaries",
            new Date().getTime() - t1.getTime()
        )

        return (
            res
                .set("Server-Timing", metrics)
                // @ts-ignore
                .status(ReturnCode.OK)
                .json(simpleUser)
        )
    } catch (e) {
        console.error(e)
        res.status(ReturnCode.INTERNAL_SERVER_ERROR).end()
    }
}

userRoutes.get("/createChallenge", createUserChallenge)
userRoutes.post("/", createUser)
userRoutes.get("/", checkLoggedIn, getUserData)
userRoutes.get("/personalizeddata", checkLoggedIn, getPersonalizedData)
userRoutes.delete("/", checkLoggedIn, deleteUser)
userRoutes.get("/delete/:id/", deleteUserConfirm)
export default userRoutes
