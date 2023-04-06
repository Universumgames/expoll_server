package net.mt32.expoll.routes.admin

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.mt32.expoll.analytics.AnalyticsStorage
import net.mt32.expoll.analytics.getCounts

fun Route.analyticsRoutes(){
    route("/analytics"){
        get("databaseCounts"){
            call.respond(AnalyticsStorage.getCounts())
        }
        get("requestCounts"){
            call.respond(AnalyticsStorage.requestCountStorage.values.map { it.toResponse() })
        }
        get("notificationCounts"){
            call.respond(AnalyticsStorage.notificationCount)
        }
    }
}