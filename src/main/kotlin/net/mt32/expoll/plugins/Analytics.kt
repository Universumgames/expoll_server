package net.mt32.expoll.plugins

import io.ktor.server.application.*
import io.ktor.server.request.*
import net.mt32.expoll.analytics.AnalyticsStorage
import net.mt32.expoll.analytics.registerRequest
import net.mt32.expoll.helper.ReturnCode

val AnalyticsPlugin = createApplicationPlugin("analytics") {
    onCallRespond { call ->
        if (call.response.status() != ReturnCode.BAD_REQUEST && call.response.status()?.value != 404)
            AnalyticsStorage.registerRequest(call.request.uri.substringBefore("?"))
    }
}

fun Application.configureAnalytics() {
    install(AnalyticsPlugin)
}