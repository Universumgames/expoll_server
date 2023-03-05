package net.mt32.expoll

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import net.mt32.expoll.entities.User
import net.mt32.expoll.plugins.*

fun main(args: Array<String>){
    ConfigLoader.load("development")
    DatabaseFactory.init()
    val user = User.loadFromID("4411a4b1-f62a-11ec-bd56-0242ac190002")
    println(user?.mail)
    println(user?.votes)
    println(user?.votes?.get(0))
    val vote = user?.votes?.get(0)
    vote?.votedFor = VoteValue.MAYBE
    vote?.save()
    println(vote)
    println(user?.votes?.get(0))

    //embeddedServer(Netty, port = config.serverPort, host = "0.0.0.0", module = Application::module)
    embeddedServer(Netty, port = 7070, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    configureSecurity()
    configureHTTP()
    configureSerialization()
    configureRouting()
}
