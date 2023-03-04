package net.mt32.expoll.entities

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*

@Serializable
data class User(val id: String, val username: String, val firstName: String, val lastName: String)

object Users: Table(){
    val id = uuid("id")
    val username = varchar("username", 255)

    override val primaryKey = PrimaryKey(id)
}