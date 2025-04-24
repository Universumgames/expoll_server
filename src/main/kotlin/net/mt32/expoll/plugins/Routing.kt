package net.mt32.expoll.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.mt32.expoll.commons.helper.ReturnCode
import net.mt32.expoll.routes.apiRouting

fun Application.configureRouting() {
    install(StatusPages) {
        exception<NotImplementedError>{call, notImplementedError ->
            notImplementedError.printStackTrace()
            call.respondText(text = "501: ${notImplementedError.message}", status = ReturnCode.NOT_IMPLEMENTED)
        }
        exception<BadRequestException>{ call, _ ->
            call.respond(ReturnCode.MISSING_PARAMS)
        }
        exception<Throwable> { call, cause ->
            cause.printStackTrace()
            call.respondText(text = "500: $cause" , status = HttpStatusCode.InternalServerError)
        }
        exception<Exception>{call, exception ->
            exception.printStackTrace()
            call.respondText(text = "500: $exception" , status = HttpStatusCode.InternalServerError)
        }

        status(HttpStatusCode.TooManyRequests) { call, status ->
            val retryAfter = call.response.headers["Retry-After"]
            call.respondText(text = "429: Too many requests. Wait for $retryAfter seconds.", status = status)
        }
    }
    routing {
        apiRouting()
    }
}

fun Route.query(body: suspend RoutingContext.() -> Unit): Route {
    return method(HttpMethod.Query) { handle(body) }
}

