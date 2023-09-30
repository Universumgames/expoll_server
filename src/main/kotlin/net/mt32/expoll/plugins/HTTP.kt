package net.mt32.expoll.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.plugins.forwardedheaders.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*
import net.mt32.expoll.helper.ServerTimingsHeader

fun Application.configureHTTP() {
    install(DefaultHeaders) {
        //header("X-Engine", "Ktor") // will send this header with each response
        header(HttpHeaders.Server, "expoll_backend")
        header(HttpHeaders.AccessControlAllowOrigin, "*")
    }
    routing {
        //openAPI(path = "openapi", swaggerFile = "openapi/openapi.yaml")
        swaggerUI(path = "openapi", swaggerFile = "openapi/openapi.yaml") {
            version = "4.15.5"
        }
        //userRoutes()
    }
    install(DoubleReceive)
    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024) // condition
        }
    }
    install(ForwardedHeaders)
    install(ServerTimingsHeader)
    install(CORS){
        anyHost()
        allowHeaders { true }
        allowCredentials = true
        allowNonSimpleContentTypes = true
        allowSameOrigin = true
        allowXHttpMethodOverride()
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
    }
}
