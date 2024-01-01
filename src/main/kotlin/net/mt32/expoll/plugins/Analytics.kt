package net.mt32.expoll.plugins

import io.ktor.server.application.*
import io.ktor.server.request.*
import net.mt32.expoll.analytics.AnalyticsStorage
import net.mt32.expoll.analytics.registerRequest
import net.mt32.expoll.helper.ReturnCode
import net.mt32.expoll.notification.ExpollNotificationHandler

val AnalyticsPlugin = createApplicationPlugin("analytics") {
    onCallRespond { call ->
        if (call.response.status() != ReturnCode.BAD_REQUEST) {
            AnalyticsStorage.registerRequest(call.request.uri.substringBefore("?"))
            AnalyticsStorage.registerRequest("*")
        }

        if ((call.response.status()?.value ?: 0) in 500..599) {
            // TODO add error to analytics
            // TODO improve error notification
            ExpollNotificationHandler.sendInternalErrorNotification(call.response.toString())
        }
    }
}

fun Application.configureAnalytics() {
    install(AnalyticsPlugin)
}