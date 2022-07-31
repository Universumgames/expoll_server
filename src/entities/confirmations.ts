/* eslint-disable new-cap */
import { Entity, Column, PrimaryGeneratedColumn, BaseEntity, ManyToOne, JoinTable } from "typeorm"
import { User } from "./user"

@Entity()
/**
 * List containing user deletion requests
 */
export class DeleteConfirmation extends BaseEntity {
    @PrimaryGeneratedColumn("uuid")
    id: string

    @ManyToOne((type) => User, (user) => user.sessions)
    @JoinTable()
    user: User

    @Column({ type: "datetime" })
    expiration: Date
}
