import { IPollOption, IPollOptionDate, IPollOptionDateTime } from "expoll-lib/interfaces"
/* eslint-disable new-cap */
import { Entity, Column, PrimaryGeneratedColumn, BaseEntity, ManyToOne } from "typeorm"
import { tOptionId } from "expoll-lib/interfaces"
import { Poll } from "./poll"

/**
 * Base class for a poll option to vote for
 */
export abstract class PollOption extends BaseEntity implements IPollOption {
    @ManyToOne((type) => Poll, (poll) => poll.id, { nullable: false, onDelete: "CASCADE" })
    poll: Poll

    @PrimaryGeneratedColumn()
    id: tOptionId
}

@Entity()
/**
 * Poll option for a poll with strings
 */
export class PollOptionString extends PollOption implements IPollOption {
    @Column()
    value: string
}

@Entity()
/**
 * Poll Option with date (end optional)
 */
export class PollOptionDate extends PollOption implements IPollOptionDate {
    @Column({ type: "date" })
    dateStart: Date

    @Column({ nullable: true, type: "date" })
    dateEnd?: Date
}

@Entity()
/**
 * Poll option wht datetime (end optional)
 */
export class PollOptionDateTime extends PollOption implements IPollOptionDateTime {
    @Column({ type: "datetime" })
    dateTimeStart: Date

    @Column({ nullable: true, type: "datetime" })
    dateTimeEnd?: Date
}
