/* eslint-disable new-cap */
import { Entity, BaseEntity, Column, PrimaryGeneratedColumn } from "typeorm"
import { MailRegexEntry } from "expoll-lib"

@Entity()
/**
 * List containing regex rules for disallowed mail addresses
 */
export class MailRegexRules extends BaseEntity implements MailRegexEntry {
    @PrimaryGeneratedColumn("uuid")
    id: string

    @Column()
    regex: string

    @Column()
    blacklist: boolean
}
