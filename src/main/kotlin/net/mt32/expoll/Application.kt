package net.mt32.expoll

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.sessions.*
import io.ktor.util.*
import net.mt32.expoll.database.DatabaseFactory
import net.mt32.expoll.plugins.*

fun main(args: Array<String>) {
    val environment = if (args.isEmpty()) "" else args[0]
    if (args.isEmpty())
        println("Define an environment to load the config from by providing it as the first argument")
    ConfigLoader.load(environment)
    DatabaseFactory.init()

    // TODO ensure testuser access


    embeddedServer(Netty, port = config.serverPort, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    configureSecurity()
    configureHTTP()
    configureSerialization()
    configureRouting()
}
