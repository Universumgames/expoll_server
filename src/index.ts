/* eslint-disable no-unused-vars */
import Database from "./database"
// import http from "http"
import Router from "./router"
import apiRoutes from "./routes/apiroutes"
import { createUserManager } from "./UserManagement"
import { createPollManager } from "./PollManagement"
import { createMailManager } from "./MailManager"
import { config } from "./expoll_config"
import { createNotificationManager } from "./NotificationManager"

const db = new Database()
db.init()

const userManager = createUserManager(db)
const pollManager = createPollManager(db)
const mailManager = createMailManager()
const notificationManager = createNotificationManager()

/**
 * Test mail connection
 */
async function test() {
    const mailTest = await mailManager.testConnection()
    console.log("Connection to mail server is " + (mailTest ? "successful" : "not successful"))
}

test()

userManager.ensureTestUser()

const router = new Router(config.serverPort)

router.addRoutes("/", apiRoutes)
router.startServer()
