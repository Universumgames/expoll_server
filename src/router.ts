import http from "http"
import express, { Express } from "express"
import cookieParser from "cookie-parser"

/**
 * Main class for managing http routes
 */
export default class Router {
    private port: number
    private httpServer: any
    private routes: express.Router
    private router: Express
    private isSetUp: Boolean = false

    /**
     * Creates new Router object
     * @param {int} port the port the server should listen on when calling startServer
     */
    constructor(port: number) {
        this.port = port
        this.router = express()
        // eslint-disable-next-line new-cap
        this.routes = express.Router()
    }

    /**
     * Creates server
     */
    private setup(): void {
        this.router.use(cookieParser())
        this.router.use(express.json())
        this.router.use("/", this.routes)
        this.httpServer = http.createServer(this.router)
        this.isSetUp = true
    }

    /**
     * Starts server, listening on port
     */
    startServer(): void {
        if (!this.isSetUp) {
            // console.error("Router is not fully setup, setting up manually (may cause problems)")
            this.setup()
        }
        this.httpServer.listen(this.port, () => {
            console.log(`Server is running on http://localhost:${this.port}`)
        })
    }

    /**
     * Adding new routes to /
     * @param {string} path the path the routes should be added to
     * @param {express.Router} routes routes in a tree-like structure
     */
    addRoutes(path: string, routes: express.Router) {
        this.routes.use(path, routes)
    }
}
