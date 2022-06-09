import { Buffer } from "node:buffer"
import { User } from "./user"
import {
    Entity,
    Column,
    PrimaryGeneratedColumn,
    BaseEntity,
    ManyToMany,
    OneToMany,
    JoinTable,
    PrimaryColumn,
    ManyToOne,
    OneToOne
} from "typeorm"
import { encode } from "punycode"
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

    /**
     * decode from sql stored format
     * @return {any}
     */
    getCredentialID(): ArrayBuffer {
        return Buffer.from(this.credentialID, "base64")
    }

    /**
     * encode to sql stored format
     * @param {any} credentialID
     */
    setCredentialID(credentialID: ArrayBuffer) {
        this.credentialID = Buffer.from(credentialID).toString("base64")
    }

    /**
     * decode from sql stored format
     * @return {any}
     */
    getCredentialPublicKey(): ArrayBuffer {
        return Buffer.from(this.credentialPublicKey, "base64")
    }

    /**
     * encode to sql stored format
     * @param {any} credentialPublicKey
     */
    setCredentialPublicKey(credentialPublicKey: ArrayBuffer) {
        this.credentialPublicKey = Buffer.from(credentialPublicKey).toString("base64")
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
