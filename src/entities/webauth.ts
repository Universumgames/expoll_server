import { Buffer } from "node:buffer"
import { User } from "./user"
import { Entity, Column, PrimaryGeneratedColumn, BaseEntity, PrimaryColumn, ManyToOne } from "typeorm"
import { base64URLStringToBuffer, bufferToBase64URLString } from "../helper"
/* eslint-disable new-cap */

@Entity()
/**
 * Challenge data
 */
export class Challenge extends BaseEntity {
    @PrimaryGeneratedColumn()
    id: string

    @Column()
    challenge: string

    @ManyToOne((type) => User, (user) => user.challenges)
    user: User
}

@Entity()
/**
 * Authenticators
 */
export class Authenticator extends BaseEntity {
    @ManyToOne((type) => User, (user) => user.authenticators)
    user: User

    @PrimaryColumn()
    credentialID: string

    @Column()
    credentialPublicKey: string

    @Column()
    counter: number

    @Column()
    transports: string

    @Column()
    name: string

    @Column()
    initiatorPlatform: string

    @Column()
    created: Date

    /**
     * decode from sql stored format
     * @return {any}
     */
    getCredentialID() {
        return base64URLStringToBuffer(this.credentialID)
    }

    /**
     * encode to sql stored format
     * @param {any} credentialID
     */
    setCredentialID(credentialID: any) {
        this.credentialID = bufferToBase64URLString(credentialID)
    }

    /**
     * decode from sql stored format
     * @return {any}
     */
    getCredentialPublicKey() {
        return base64URLStringToBuffer(this.credentialPublicKey)
    }

    /**
     * encode to sql stored format
     * @param {any} credentialPublicKey
     */
    setCredentialPublicKey(credentialPublicKey: any) {
        this.credentialPublicKey = bufferToBase64URLString(credentialPublicKey)
    }

    /**
     *
     * @return {AuthenticatorTransport[]} convert transports string to usable enum
     */
    getAuthenticatorTransports(): AuthenticatorTransport[] {
        const splitted = this.transports.split(",")
        const arr = []
        for (const split of splitted) {
            if (split == "usb") arr.push(AuthenticatorTransport.usb)
            if (split == "ble") arr.push(AuthenticatorTransport.ble)
            if (split == "nfc") arr.push(AuthenticatorTransport.nfc)
            if (split == "internal") arr.push(AuthenticatorTransport.internal)
        }
        return arr
    }

    /**
     *
     * @param {AuthenticatorTransports[]} transports the transports to convert to sql format
     */
    setAuthenticatorTransports(transports: AuthenticatorTransport[]) {
        this.transports = transports
            .map((transport) => {
                switch (transport) {
                    case AuthenticatorTransport.usb:
                        return "usb"
                    case AuthenticatorTransport.ble:
                        return "ble"
                    case AuthenticatorTransport.nfc:
                        return "nfc"
                    case AuthenticatorTransport.internal:
                        return "internal"
                }
                return ""
            })
            .join(",")
    }
}

export enum AuthenticatorTransport {
    usb = "usb",
    ble = "ble",
    nfc = "nfc",
    internal = "internal"
}
