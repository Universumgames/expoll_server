/* eslint-disable new-cap */
import { NotificationPreferences } from "expoll-lib"
import { Entity, BaseEntity, Column, OneToOne, PrimaryGeneratedColumn, JoinColumn } from "typeorm"
import { User } from "./user"

@Entity()
/**
 * List containing user-apple-device combinations for notifications
 */
export class NotificationPreferencesEntity extends BaseEntity implements NotificationPreferences {
    @PrimaryGeneratedColumn("uuid")
    id: string

    @JoinColumn()
    @OneToOne((type) => User)
    user: User

    @Column({ default: true })
    voteChange: boolean
    @Column({ default: true })
    userAdded: boolean
    @Column({ default: true })
    userRemoved: boolean
    @Column({ default: true })
    pollDeleted: boolean
    @Column({ default: true })
    pollEdited: boolean
    @Column({ default: true })
    pollArchived: boolean
}
