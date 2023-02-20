import { IPoll, PollType } from "expoll-lib/interfaces"
/* eslint-disable new-cap */
import { v4 as uuidv4 } from "uuid"
import {
    Entity,
    Column,
    BaseEntity,
    PrimaryColumn,
    ManyToOne,
    CreateDateColumn,
    UpdateDateColumn,
    OneToMany,
    JoinTable
} from "typeorm"
import { tPollID } from "expoll-lib/interfaces"
import { User } from "./user"
import { Vote } from "./vote"
import { PollUserNote } from "./note"

@Entity()
/**
 * Poll meta data, except vote options and votes
 */
export class Poll extends BaseEntity implements IPoll {
    @ManyToOne((type) => User, (user) => user.polls, { nullable: false })
    @JoinTable()
    admin: User

    @PrimaryColumn()
    id: tPollID = Poll.generatePollID()

    /**
     * Generate new random poll id
     * @return {String} new poll id
     */
    private static generatePollID(): string {
        return uuidv4()
    }

    @Column()
    name: string

    @CreateDateColumn()
    created: Date

    @UpdateDateColumn()
    updated: Date

    @Column({ type: "text" })
    description: string

    static MAX_DESCRIPTION_LENGTH = 65535

    @Column()
    type: PollType = PollType.String

    @OneToMany((type) => Vote, (vote) => vote.poll, { onDelete: "CASCADE" })
    @JoinTable()
    votes: Vote[]

    @OneToMany((type) => PollUserNote, (note) => note.poll, { onDelete: "CASCADE" })
    @JoinTable()
    notes: PollUserNote[]

    @Column()
    /**
     * sets the number votes each user can send for this poll
     * use a number <= 0 to set it to infinity
     */
    maxPerUserVoteCount: number = -1

    @Column()
    allowsMaybe: boolean = true

    @Column()
    allowsEditing: boolean = true
}
