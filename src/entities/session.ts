import { ISession } from "expoll-lib/interfaces"
/* eslint-disable new-cap */
import { v4 as uuidv4 } from "uuid"
import { Entity, Column, BaseEntity, PrimaryColumn, ManyToOne, JoinTable } from "typeorm"
import { User } from "./user"

@Entity()
/**
 * Session ibject to store users session data
 */
export class Session extends BaseEntity implements ISession {
    @PrimaryColumn()
    loginKey: string = Session.generateLoginKey()

    @Column({ type: "datetime" })
    expiration: Date

    @Column({ nullable: true })
    userAgent: string

    @ManyToOne((type) => User, (user) => user.sessions)
    @JoinTable()
    user: User

    /**
     * Generate new random login key
     * @return {String} new login key
     */
    private static generateLoginKey(): string {
        return uuidv4()
    }

    /**
     * check wether the session is not expired
     * @return {boolean} returns true if session is valid
     */
    isValid(): boolean {
        return this.expiration > new Date()
    }
}
