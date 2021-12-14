import http from "http"
import express, { Express } from "express"
export default class Router {
    private port: number
    private httpServer: any
    private routes: express.Router
    private router: Express
    private isSetUp: Boolean = false

    constructor(port: number) {
        this.port = port
        this.router = express()
        this.routes = express.Router()
    }

    private setup(): void {
        this.router.use("/", this.routes)
        this.httpServer = http.createServer(this.router)
        this.isSetUp = true
    }

    startServer(): void {
        if (!this.isSetUp) {
            console.error("Router is not fully setup, setting up manually (may cause problems)")
            this.setup()
        }
        this.httpServer.listen(this.port, () => console.log(`Server is running on https://localhost:${this.port}`))
    }

    addRoutes(path: string, routes: express.Router) {
        this.routes.use(path, routes)
    }
}
