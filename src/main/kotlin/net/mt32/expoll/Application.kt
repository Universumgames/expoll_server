package net.mt32.expoll

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import net.mt32.expoll.auth.OIDC
import net.mt32.expoll.commons.helper.UnixTimestamp
import net.mt32.expoll.commons.helper.getMillisToMidnight
import net.mt32.expoll.database.DatabaseFactory
import net.mt32.expoll.entities.OTP
import net.mt32.expoll.entities.Session
import net.mt32.expoll.entities.User
import net.mt32.expoll.entities.notifications.APNDevice
import net.mt32.expoll.notification.ExpollNotificationHandler
import net.mt32.expoll.plugins.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.concurrent.thread

private val timer: Timer = Timer()

fun main(args: Array<String>) {
    val environment = if (args.isEmpty()) "default" else args[0]
    if (args.isEmpty())
        println("Define an environment to load the config from by providing it as the first argument")
    ConfigLoader.load(environment)
    DatabaseFactory.init()
    val testUserThread = thread { User.ensureTestUserExistence() }
    val oidcThread = thread { runBlocking { OIDC.init() } }
    initCleanup()

    testUserThread.join()
    oidcThread.join()
    println("Server initialisation finished")
    sendStartupNotification()
    println(DatabaseFactory.db != null)

    embeddedServer(Netty, port = config.serverPort, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    println(DatabaseFactory.db != null)
    configureSecurity()
    configureHTTP()
    configureSerialization()
    configureRouting()
    configureAnalytics()
}

private fun initCleanup() {
    val now = Calendar.getInstance()
    val delay = getMillisToMidnight(now)
    cleanupCoroutine()
    timer.schedule(object : TimerTask() {
        override fun run() {
            cleanupCoroutine()
        }
    }, delay, UnixTimestamp.zero().addDays(1).millisSince1970)
}

/***
 * Cleanup all expired sessions, OTPs, APN devices and deactivate inactive users
 */
private fun cleanupCoroutine() {
    // clean otp
    transaction {
        OTP.all().forEach { if (!it.valid) it.delete() }
    }
    // clean session
    transaction {
        Session.all().forEach {
            if (!it.isValid)
                it.delete()
        }
    }
    // clean apn devices
    transaction {
        APNDevice.all().forEach { if (!it.isValid) it.delete() }
    }
    // notify users of inactivity
    transaction {
        val longLastLoginUsers = User.oldLoginUsers()
        longLastLoginUsers.forEach { it.deactivateUser() }

        val toDelete = User.usersToDelete()
        toDelete.forEach { it.anonymizeUserData() }

        val finalDelete = User.usersToFinalDelete()
        finalDelete.forEach { it.finalDelete() }
    }
}

private fun sendStartupNotification() {
    ExpollNotificationHandler.sendServerStartup()
}