package net.mt32.expoll

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import net.mt32.expoll.auth.OIDC
import net.mt32.expoll.database.DatabaseFactory
import net.mt32.expoll.entities.OTP
import net.mt32.expoll.entities.Session
import net.mt32.expoll.entities.User
import net.mt32.expoll.helper.UnixTimestamp
import net.mt32.expoll.helper.getDelayToMidnight
import net.mt32.expoll.plugins.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

private val timer: Timer = Timer()

fun main(args: Array<String>) {
    val environment = if (args.isEmpty()) "" else args[0]
    if (args.isEmpty())
        println("Define an environment to load the config from by providing it as the first argument")
    ConfigLoader.load(environment)
    DatabaseFactory.init()
    User.ensureTestUserExistence()
    runBlocking {
        OIDC.init()
    }
    initCleanup()
    println("Server initialisation finished")

    embeddedServer(Netty, port = config.serverPort, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    configureSecurity()
    configureHTTP()
    configureSerialization()
    configureRouting()
    configureAnalytics()
}

private fun initCleanup() {
    val now = Calendar.getInstance()
    val delay = getDelayToMidnight(now)
    cleanupCoroutine()
    timer.schedule(object : TimerTask() {
        override fun run() {
            cleanupCoroutine()
        }
    }, delay, UnixTimestamp.zero().addDays(1).millisSince1970)
}

private fun cleanupCoroutine() {
    // clean otp
    transaction {
        OTP.all().forEach { if (!it.valid) it.delete() }
    }
    // clean session
    transaction {
        Session.all().forEach { if (!it.isValid) it.delete() }
    }
}