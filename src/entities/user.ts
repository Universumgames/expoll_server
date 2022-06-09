import { Authenticator, Challenge } from "./webauth"
import { IUser } from "expoll-lib/interfaces"
/* eslint-disable new-cap */
import { Entity, Column, PrimaryGeneratedColumn, BaseEntity, ManyToMany, OneToMany, JoinTable } from "typeorm"
import { tUserID } from "expoll-lib/interfaces"
import { Poll } from "./poll"
import { Vote } from "./vote"
import { Session } from "./session"
import { PollUserNote } from "./note"

@Entity()
/**
 * User object storing all user data
 */
export class User extends BaseEntity implements IUser {
    @PrimaryGeneratedColumn()
    id: tUserID

    @Column({ unique: true })
    username: string

    @Column()
    firstName: string

    @Column()
    lastName: string

    @Column({ unique: true })
    mail: string

    @ManyToMany((type) => Poll, (poll) => poll.id)
    @JoinTable()
    polls: Poll[]

    @OneToMany((type) => Vote, (vote) => vote.user)
    @JoinTable()
    votes: Vote[]

    @OneToMany((type) => Session, (session) => session.user)
    @JoinTable()
    sessions: Session[]

    @OneToMany((type) => PollUserNote, (note) => note.user)
    @JoinTable()
    notes: PollUserNote[]

    @Column({ default: true })
    active: boolean

    @Column({ default: false })
    admin: boolean

    @OneToMany((type) => Challenge, (challenge) => challenge.user)
    challenges: Challenge[]

    @OneToMany((type) => Authenticator, (auth) => auth.user)
    authenticators: Authenticator[]

    /**
     * Generate new Session that will be saved automatically
     * @return {Session} new Session
     */
    async generateSession(): Promise<Session> {
        const session = new Session()
        session.user = this
        const ex = new Date()
        ex.setMonth(ex.getMonth() + 3)
        session.expiration = ex

        await this.save()
        await session.save()

        return session
    }
}
