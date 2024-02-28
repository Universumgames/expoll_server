package net.mt32.expoll.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*

private val userAgentBlockList: List<String> = listOf("zap", "hydra")

class UserAgentBlockerConfig {
    var blockList: List<String> = userAgentBlockList
    var blockEmptyUserAgent: Boolean = true
}

private object BeforeCall : Hook<suspend (ApplicationCall) -> Unit> {
    override fun install(pipeline: ApplicationCallPipeline, handler: suspend (ApplicationCall) -> Unit) {
        val beforeCallPhase = PipelinePhase("BeforeCall")
        pipeline.insertPhaseBefore(ApplicationCallPipeline.Call, beforeCallPhase)
        pipeline.intercept(beforeCallPhase) { handler(call) }
    }
}

val UserAgentBlocker = createApplicationPlugin("user-agent-blocker", createConfiguration = ::UserAgentBlockerConfig){
    pluginConfig.blockList = pluginConfig.blockList.map { it.lowercase() }
    on(BeforeCall) { call ->
        val userAgent = call.request.headers["User-Agent"]?.lowercase()
        if (userAgent == null && pluginConfig.blockEmptyUserAgent) {
            call.respond(HttpStatusCode.TooManyRequests)
            println("Blocked empty user agent from ${call.request.origin.remoteHost}")
        }
        if(pluginConfig.blockList.any { userAgent?.contains(it) == true }) {
            call.respond(HttpStatusCode.TooManyRequests)
            println("Blocked user agent from ${call.request.origin.remoteHost} with user agent $userAgent")

        }
    }
}