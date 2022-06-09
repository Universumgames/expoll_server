import { IPollUserNote } from "expoll-lib/interfaces"
/* eslint-disable new-cap */
import { Entity, Column, PrimaryGeneratedColumn, BaseEntity, ManyToOne } from "typeorm"
import { User } from "./user"
import { Poll } from "./poll"

@Entity()
/**
 * Store Notes for a user
 */
export class PollUserNote extends BaseEntity implements IPollUserNote {
    @PrimaryGeneratedColumn()
    id: number

    @ManyToOne((type) => User, (user) => user.notes, { nullable: false })
    user: User

    @ManyToOne((type) => Poll, (poll) => poll.notes, { nullable: false, onDelete: "CASCADE" })
    poll: Poll

    @Column()
    note: string
}
