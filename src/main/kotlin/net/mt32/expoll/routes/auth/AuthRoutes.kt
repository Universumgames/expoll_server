package net.mt32.expoll.routes.auth

import io.ktor.server.routing.*

fun Route.authRoutes(){
    route("auth"){
        simpleAuthRoutes()
        webauthnRoutes()
    }
}