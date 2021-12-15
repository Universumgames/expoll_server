import { createConnection } from "typeorm"
import { User } from "./interfaces"

/**
 * Database class to manage access to database
 */
export default class Database {
    /**
     * Create basic database object
     */
    constructor() {}

    /**
     * Initialise databse
     */
    async init() {
        try {
            const connection = await createConnection({
                type: "mariadb",
                host: "localhost",
                port: 3306,
                username: "root",
                password: "password",
                database: "expoll",
                // eslint-disable-next-line no-undef
                entities: [__dirname + "/interfaces.js"],
                synchronize: true,
            })
            const repo = connection.getRepository(User)
            const u = new User()
            const u2 = await repo.findOne({ where: { mail: "test@test.com" } })
            Object.assign(u, u2)
            u.firstName = "tom"
            u.lastName = "a"
            u.mail = "test@test.com"
            u.username = "universum"
            await repo.save(u)
        } catch (e) {
            console.error("COnnection failed", e)
        }
    }
}
