/* eslint-disable new-cap */
import { Entity, Column, PrimaryGeneratedColumn, BaseEntity, ManyToOne } from "typeorm"
import { tOptionId, VoteValue } from "expoll-lib/interfaces"
import { User } from "./user"
import { Poll } from "./poll"

@Entity()
/**
 * Vote object
 */
export class Vote extends BaseEntity {
    @PrimaryGeneratedColumn()
    id: number

    @ManyToOne((type) => User, (user) => user.votes, { nullable: false })
    user: User

    @ManyToOne((type) => Poll, (poll) => poll.votes, { nullable: false })
    poll: Poll

    @Column()
    optionID: tOptionId

    @Column()
    votedFor: VoteValue = VoteValue.no
}
