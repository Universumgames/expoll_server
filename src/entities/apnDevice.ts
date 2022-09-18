/* eslint-disable new-cap */
import { Entity, BaseEntity, CreateDateColumn, PrimaryColumn, ManyToOne, JoinColumn } from "typeorm"
import { User } from "./user"

@Entity("apn_devices")
/**
 * List containing user-apple-device combinations for notifications
 */
export class APNsDevice extends BaseEntity {
    @PrimaryColumn()
    deviceID: string

    @JoinColumn()
    @ManyToOne((type) => User)
    user: User

    @CreateDateColumn()
    creation: Date
}
