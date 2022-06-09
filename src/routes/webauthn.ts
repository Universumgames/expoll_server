import { ReturnCode } from "expoll-lib/interfaces"
import { config } from "./../expoll_config"
import { Authenticator, Challenge } from "./../entities/webauth"
import { User } from "entities/user"
import { checkLoggedIn } from "./routeHelper"
import express, { CookieOptions, NextFunction, Request, Response } from "express"
import {
    generateAuthenticationOptions,
    generateRegistrationOptions,
    verifyRegistrationResponse
} from "@simplewebauthn/server/./dist"
import { AuthenticatorTransportFuture } from "@simplewebauthn/typescript-types"

// eslint-disable-next-line new-cap
const webauthnRoutes = express.Router()

/* const test = async (req: Request, res: Response, next: NextFunction) => {
    return res.status(200).json({
        message: "Test successful"
    })
} */

const registerInit = async (req: Request, res: Response, next: NextFunction) => {
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
    // delete old challenges
    await Challenge.delete({ user: user })

    const challenge = new Challenge()
    challenge.challenge = options.challenge
    challenge.user = user
    await challenge.save()

    return res.status(ReturnCode.OK).json(options)
}

const registerRes = async (req: Request, res: Response, next: NextFunction) => {
    // @ts-ignore
    const user = req.user as User
    const expectedChallenge = (await Challenge.findOne({ where: { user: user } }))?.challenge
    if (!expectedChallenge) {
        return res.status(ReturnCode.BAD_REQUEST).end()
    }

    let verification
    try {
        verification = await verifyRegistrationResponse({
            credential: req.body,
            expectedChallenge,
            expectedOrigin: origin,
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
        await auth.save()
    }
    return res.status(ReturnCode.OK).json({
        verified
    })
}

const authenticateInit = async (req: Request, res: Response, next: NextFunction) => {
    // TODO we dont have access to the user here, he is not logged in yet
    // @ts-ignore
    const user = req.user as User

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
    // delete old challenges
    await Challenge.delete({ user: user })

    const challenge = new Challenge()
    challenge.challenge = options.challenge
    challenge.user = user
    await challenge.save()

    return res.status(200).json(options)
}

const authenticateRes = async (req: Request, res: Response, next: NextFunction) => {
    return res.status(200).json({
        message: "Test successful"
    })
}

webauthnRoutes.get("/register", checkLoggedIn, registerInit)
webauthnRoutes.post("/register", checkLoggedIn, registerRes)
webauthnRoutes.get("/authenticate", authenticateInit)

export default webauthnRoutes
