import { NextFunction } from "express"
// import http from "http"
import Router from "./router"

const router = new Router(6060)

const test = async (req: Request, res: Response, next: NextFunction) => {
    return res.status(200).json({
        message: "hello",
    })
}

router.addRoutes("/", test)
router.startServer()
/* const routes = express.Router()
routes.get("/test", test)
const router: Express = express()
router.use("/", routes)

const httpServer = http.createServer(router)
const PORT: any = 6060
httpServer.listen(PORT, () => console.log(`Server is running on http://localhost:${PORT}`)) */
