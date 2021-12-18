import Database from "./database"
// import http from "http"
import Router from "./router"
import apiRoutes from "./routes/apiroutes"
import { createUserManager } from "./UserManagement"
import { createPollManager } from "./PollManagement"

const db = new Database()
db.init()

const userManager = createUserManager(db)
const pollManager = createPollManager(db)

const router = new Router(6060)

router.addRoutes("/", apiRoutes)
router.startServer()
/* const routes = express.Router()
routes.get("/test", test)
const router: Express = express()
router.use("/", routes)

const httpServer = http.createServer(router)
const PORT: any = 6060
httpServer.listen(PORT, () => console.log(`Server is running on http://localhost:${PORT}`)) */
