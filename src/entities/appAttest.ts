/* eslint-disable new-cap */
import { Entity,
    BaseEntity,
    CreateDateColumn,
    Column,
    PrimaryGeneratedColumn } from "typeorm"
import { Buffer } from "buffer"

@Entity("appleAppAttests")
/**
 * List containing user-apple-device combinations for notifications
 */
export class AppAttests extends BaseEntity {
    @PrimaryGeneratedColumn("uuid")
    uuid: string

    @Column()
    challenge: string

    @CreateDateColumn()
    createdAt: Date
}


export interface IAppleAppAttest {
    fmt: string
    attStmt: {
        x5c: Buffer[]
        receipt: Buffer
    }
    authData: Buffer
}
