/* eslint-disable no-unused-vars */
import { config } from "./expoll_config"
import * as jose from "jose"
import * as fs from "fs"
import axios, { AxiosRequestHeaders, AxiosResponse } from "axios"
import http2 from "http2"

/**
 * Import APNs key for JWT
 * @return {KeyLike}
 */
async function getAPNsKey(): Promise<jose.KeyLike> {
    const algorithm = "ES256"
    const key = fs.readFileSync(config.notifications.apnsKeyPath).toString()

    return await jose.importPKCS8(key, algorithm)
}

/**
 * create bearer jwt to send Notification to APNs
 * @return {String} bearer token string to send notification to APNs
 */
async function apnsBearerToken(): Promise<string> {
    const jwt = await new jose.SignJWT({ iss: config.notifications.teamID })
        .setProtectedHeader({ alg: "ES256", kid: config.notifications.apnsKeyID })
        .setIssuedAt()
        .sign(await getAPNsKey())
    return jwt
}

let apnsBearer: string | undefined = undefined
let bearerAge: Date | undefined = undefined

/**
 * Apple Push Notification type
 */
export enum APNsPushType {
    /** Normal push notification */
    alert = "alert",
    /** Silent notification with no user interaction */
    background = "background",
    /** Silent notification with location update */
    location = "location",
    /** VoIP notification */
    voip = "voip",
    /** Notification for Apple Watch complication */
    complication = "complication",
    /** Notification for File Provider */
    fileprovider = "fileprovider",
    /** Notification for Mobile Device Management */
    mdm = "mdm"
}

export enum APNsPriority {
    /** send the notification immediately */
    high = 10,
    /** send the notification based on power considerations on the user’s device */
    medium = 5,
    /** prioritize the device’s power considerations over all other
     * factors for delivery, and prevent awakening the device */
    low = 1
}

export interface APNsNotification {
    title?: string
    subtitle?: string
    body?: string
    "launch-image"?: string
    "title-loc-key"?: string
    "title-loc-args"?: string[]
    "subtitle-loc-key"?: string
    "subtitle-loc-args"?: string[]
    "loc-key"?: string
    "loc-args"?: string[]
}

export interface APNsPayload {
    aps: {
        alert: APNsNotification
        badge?: number
        sound?: string | { [x: string | number | symbol]: unknown }
        "interruption-level"?: "passive" | "active" | "time-sensitive" | "critical"
    }
    [x: string | number | symbol]: unknown
}

let apnsConnection!: http2.ClientHttp2Session

/**
 * Send a notification to
 * @param {string} deviceToken the device token, the notification should be send to
 * @param {Date} expiration the expiration date of the notification
 * @param {APNsPayload} payload the notification payload
 * @param {APNsPriority} priority the priority of the notification
 * @param {APNsPushType} pushType type of notification
 * @param {String} collapseID identifier to group notifications, max 8 characters
 */
export async function sendAPN(
    deviceToken: string,
    expiration: Date,
    payload: APNsPayload,
    priority: APNsPriority,
    pushType: APNsPushType = APNsPushType.alert,
    collapseID: string | undefined = undefined
): Promise<AxiosResponse | undefined> {
    if (
        bearerAge == undefined ||
        apnsBearer == undefined ||
        bearerAge.getTime() + 30 * 60 * 1000 < new Date().getTime()
    ) {
        apnsBearer = await apnsBearerToken()
        bearerAge = new Date()
    }

    if (apnsConnection == undefined || apnsConnection.destroyed || apnsConnection.closed) {
        apnsConnection = http2.connect(config.notifications.apnsURL)
        apnsConnection.on("error", (err) => {
            console.warn(err)
            apnsConnection = http2.connect(config.notifications.apnsURL)
        })
    }

    const body = payload

    let headers: http2.OutgoingHttpHeaders = {
        ":scheme": "https",
        ":method": "POST",
        authorization: "bearer " + apnsBearer,
        ":path": "/3/device/" + deviceToken,
        "apns-push-type": pushType,
        "apns-expiration": "0", // expiration.getTime().toString(),
        "apns-priority": priority.toString(),
        "apns-topic": config.notifications.bundleID
        // body: JSON.stringify(body)
    }
    headers = Object.assign(headers, collapseID != undefined && { "apns-collapse-id": collapseID.substring(0, 8) })

    const request = apnsConnection.request(headers)
    try {
        request.write(JSON.stringify(body), "utf8")

        request.end()
    } catch (err) {
        console.warn(err)
    }

    const responseHeaders = await new Promise<http2.IncomingHttpHeaders>((resolve, reject) => {
        request.on("response", (headers, flags) => {
            resolve(headers)
        })
    })

    request.setEncoding("utf8")
    const responseBody = await new Promise<string>((resolve, reject) => {
        let data = ""
        request.on("data", (chunk) => {
            data += chunk
        })
        request.on("end", () => {
            resolve(data)
        })
    })

    console.log(responseHeaders)
    console.log(responseBody)
    return undefined
}
