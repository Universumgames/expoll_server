/* eslint-disable no-unused-vars */
import Database from "./database"
// import http from "http"
import Router from "./router"
import apiRoutes from "./routes/apiroutes"
import { createUserManager } from "./UserManagement"
import { createPollManager } from "./PollManagement"
import { createMailManager } from "./MailManager"
import { config } from "./config"

const db = new Database()
db.init()

const userManager = createUserManager(db)
const pollManager = createPollManager(db)
const mailManager = createMailManager()

/**
 * Test mail connection
 */
async function test() {
    const mailTest = await mailManager.testConnection()
    console.log("Connection to mail server is " + (mailTest ? "successful" : "not successful"))
}

test()

const router = new Router(config.serverPort)

router.addRoutes("/", apiRoutes)
router.startServer()
/* const routes = express.Router()
routes.get("/test", test)
const router: Express = express()
router.use("/", routes)

const httpServer = http.createServer(router)
const PORT: any = 6060
httpServer.listen(PORT, () => console.log(`Server is running on http://localhost:${PORT}`)) */
