import { ReturnCode } from "expoll-lib/interfaces"
import { config } from "../../expoll_config"
import { Authenticator } from "../../entities/webauth"
import { User } from "../../entities/user"
import { checkLoggedIn } from "../routeHelper"
import express, { NextFunction, Request, Response } from "express"
import {
    generateAuthenticationOptions,
    generateRegistrationOptions,
    verifyAuthenticationResponse,
    verifyRegistrationResponse
} from "@simplewebauthn/server"
import { AuthenticatorTransportFuture } from "@simplewebauthn/typescript-types"
import { cookieConfig, cookieName, getUserRequest, toBuffer } from "../../helper"
import getUserManager from "../../UserManagement"
import { CreateUserResponse } from "expoll-lib"

// eslint-disable-next-line new-cap
const webauthnRoutes = express.Router()

/* const test = async (req: Request, res: Response, next: NextFunction) => {
    return res.status(200).json({
        message: "Test successful"
    })
} */

const registerInit = async (req: Request, res: Response, next: NextFunction) => {
    try {
        // @ts-ignore
        const user = req.user as User

        const authenticators = await Authenticator.find({ relations: ["user"], where: { user: user } })

        const rpName = config.webauthn.rpName
        const rpID = config.webauthn.rpID

        const options = generateRegistrationOptions({
            rpName,
            rpID,
            userID: user.id.toString(),
            userName: user.username,
            // Don't prompt users for additional information about the authenticator
            // (Recommended for smoother UX)
            attestationType: "direct",
            // Prevent users from re-registering existing authenticators
            excludeCredentials: authenticators.map((authenticator) => ({
                id: authenticator.getCredentialID(),
                type: "public-key",
                // Optional
                transports: authenticator.getAuthenticatorTransports() as AuthenticatorTransportFuture[]
            }))
        })
        await getUserManager().setCurrentUserChallenge(user, options.challenge)

        // @ts-ignore
        options.userVerification = "preferred"

        return res.status(ReturnCode.OK).json(options)
    } catch (error: any) {
        return res.status(ReturnCode.INTERNAL_SERVER_ERROR).json({ error })
    }
}

const registerRes = async (req: Request, res: Response, next: NextFunction) => {
    try {
        // @ts-ignore
        const user = req.user as User
        const expectedChallenge = (await getUserManager().getCurrentUserChallenge(user))?.challenge
        if (!expectedChallenge) {
            return res.status(ReturnCode.BAD_REQUEST).end()
        }

        let verification
        try {
            verification = await verifyRegistrationResponse({
                credential: req.body,
                expectedChallenge,
                expectedOrigin: config.webauthn.origin,
                expectedRPID: config.webauthn.rpID
            })
        } catch (error: any) {
            console.error(error)
            return res.status(ReturnCode.BAD_REQUEST).send({ error: error.message })
        }

        const { verified } = verification

        if (verified) {
            const { registrationInfo } = verification
            if (!registrationInfo) {
                return res.status(ReturnCode.BAD_REQUEST).end()
            }
            const { credentialPublicKey, credentialID, counter } = registrationInfo

            const auth = new Authenticator()
            auth.setCredentialID(credentialID)
            auth.counter = counter
            auth.setCredentialPublicKey(credentialPublicKey)
            auth.setAuthenticatorTransports(req.body.transports ?? [])
            auth.user = user
            // @ts-ignore
            auth.name = req.headers["user-agent"]
            // @ts-ignore
            auth.initiatorPlatform = req.headers["user-agent"]
            auth.created = new Date()
            await auth.save()
        }
        return res.status(ReturnCode.OK).json({
            verified
        })
    } catch (error: any) {
        return res.status(ReturnCode.INTERNAL_SERVER_ERROR).json({ error })
    }
}

const authenticateInit = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const userReq = getUserRequest(req)

        let user: User | undefined = undefined
        if (userReq.username != undefined) user = await getUserManager().getUser({ username: userReq.username })
        if (userReq.mail != undefined) user = await getUserManager().getUser({ mail: userReq.mail })
        if (!user) return res.status(ReturnCode.INVALID_PARAMS).end()

        const authenticators = await Authenticator.find({ relations: ["user"], where: { user: user } })

        const options = generateAuthenticationOptions({
            // Require users to use a previously-registered authenticator
            allowCredentials: authenticators.map((authenticator) => ({
                id: authenticator.getCredentialID(),
                type: "public-key",
                // Optional
                transports: authenticator.getAuthenticatorTransports()
            })),
            userVerification: "preferred"
        })

        await getUserManager().setCurrentUserChallenge(user, options.challenge)

        // @ts-ignore
        options.userVerification = "preferred"

        return res.status(ReturnCode.OK).json(options)
    } catch (error: any) {
        return res.status(ReturnCode.INTERNAL_SERVER_ERROR).json({ error })
    }
}

const authenticateRes = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const userReq = getUserRequest(req)
        let user: User | undefined = undefined
        if (userReq.username != undefined) user = await getUserManager().getUser({ username: userReq.username })
        if (userReq.mail != undefined) user = await getUserManager().getUser({ mail: userReq.mail })
        if (!user) return res.status(ReturnCode.INVALID_PARAMS).end()

        const expectedChallenge = (await getUserManager().getCurrentUserChallenge(user))?.challenge

        if (!expectedChallenge) return res.status(ReturnCode.INVALID_CHALLENGE_RESPONSE).end()
        // (Pseudocode} Retrieve an authenticator from the DB that
        // should match the `id` in the returned credential
        // const authenticator = getUserAuthenticator(user, body.id)
        const authenticator = await Authenticator.findOne({
            relations: ["user"],
            where: { user: user, credentialID: req.body.id }
        })

        /* console.log("Request credid", req.body.id)
    const authenticators = await Authenticator.find({ relations: ["user"], where: { user: user } })
    for (const auth of authenticators) {
        console.log("Acceptable credid", auth.credentialID)
    }

    console.log("found auth", authenticator) */

        if (!authenticator) {
            return res
                .status(ReturnCode.UNAUTHORIZED)
                .json(`Could not find authenticator ${req.body.id} for user ${user.id}`)
        }

        let verification
        try {
            verification = await verifyAuthenticationResponse({
                credential: req.body,
                expectedChallenge,
                expectedOrigin: config.webauthn.origin,
                expectedRPID: config.webauthn.rpID,
                authenticator: {
                    credentialPublicKey: toBuffer(authenticator.getCredentialPublicKey()),
                    credentialID: toBuffer(authenticator.getCredentialID()),
                    counter: authenticator.counter,
                    transports: authenticator.getAuthenticatorTransports()
                }
            })

            const { authenticationInfo } = verification
            const { newCounter } = authenticationInfo
            authenticator.counter = newCounter
            await authenticator.save()
        } catch (error: any) {
            console.error(error)
            return res.status(ReturnCode.UNAUTHORIZED).send({ error: error.message }).end()
        }

        const { verified } = verification

        if (verified) {
            const loginKey = await user.generateSession()
            const data: CreateUserResponse = {
                loginKey: loginKey.loginKey
            }
            return res.status(ReturnCode.OK).cookie(cookieName, data, cookieConfig(loginKey)).json({ verified })
        } else return res.status(ReturnCode.INVALID_CHALLENGE_RESPONSE).json({ verified })
    } catch (error: any) {
        return res.status(ReturnCode.INTERNAL_SERVER_ERROR).json({ error })
    }
}

const list = async (req: Request, res: Response, next: NextFunction) => {
    try {
        // @ts-ignore
        const user = req.user as User
        const authenticators = await Authenticator.find({ where: { user: user } })
        // console.log(await Authenticator.find({ relations: ["user"], where: { user: user } }))

        return res.status(ReturnCode.OK).json({
            authenticators
        })
    } catch (error: any) {
        return res.status(ReturnCode.INTERNAL_SERVER_ERROR).json({ error })
    }
}

const rename = async (req: Request, res: Response, next: NextFunction) => {
    try {
        // @ts-ignore
        const user = req.user as User

        const credID = req.body.credentialID
        const newName = req.body.newName
        const authenticator = await Authenticator.findOne({ where: { user: user, credentialID: credID } })

        if (!authenticator) return res.status(ReturnCode.BAD_REQUEST).end()

        authenticator.name = newName
        await authenticator.save()

        return res.status(ReturnCode.OK).end()
    } catch (error: any) {
        return res.status(ReturnCode.INTERNAL_SERVER_ERROR).json({ error })
    }
}

const deleteAuth = async (req: Request, res: Response, next: NextFunction) => {
    try {
        // @ts-ignore
        const user = req.user as User
        const credID = req.body.credentialID
        const authenticator = await Authenticator.findOne({ where: { user: user, credentialID: credID } })

        if (!authenticator) return res.status(ReturnCode.BAD_REQUEST).end()

        await Authenticator.delete({ credentialID: credID, user: user })

        return res.status(ReturnCode.OK).end()
    } catch (error: any) {
        return res.status(ReturnCode.INTERNAL_SERVER_ERROR).json({ error })
    }
}

webauthnRoutes.get("/register", checkLoggedIn, registerInit)
webauthnRoutes.post("/register", checkLoggedIn, registerRes)
webauthnRoutes.get("/authenticate", authenticateInit)
webauthnRoutes.post("/authenticate", authenticateRes)
webauthnRoutes.get("/list", checkLoggedIn, list)
webauthnRoutes.post("/edit", checkLoggedIn, rename)
webauthnRoutes.delete("/", checkLoggedIn, deleteAuth)

export default webauthnRoutes
