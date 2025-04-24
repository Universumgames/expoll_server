package net.mt32.expoll.plugins

import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.request.*
import io.ktor.util.*
import net.mt32.expoll.analytics.AnalyticsStorage
import net.mt32.expoll.analytics.registerRequest
import net.mt32.expoll.analytics.registerRequestDuration
import net.mt32.expoll.commons.helper.ReturnCode
import net.mt32.expoll.commons.helper.UnixTimestamp
import net.mt32.expoll.notification.ExpollNotificationHandler

private val RequestStartAttributeKey = AttributeKey<Long>("RequestStart")

val AnalyticsPlugin = createApplicationPlugin("analytics") {
    on(CallSetup) { call ->
        call.attributes.put(RequestStartAttributeKey, UnixTimestamp.now().millisSince1970)
    }
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

        val requestStart = call.attributes.getOrNull(RequestStartAttributeKey)
        if (requestStart != null) {
            val duration = UnixTimestamp.now().millisSince1970 - requestStart
            AnalyticsStorage.registerRequestDuration(
                call.request.uri.substringBefore("?"),
                call.request.httpMethod.value,
                duration
            )
        }
    }
}

fun Application.configureAnalytics() {
    install(AnalyticsPlugin)
}