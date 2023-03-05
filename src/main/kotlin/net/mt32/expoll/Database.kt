package net.mt32.expoll

import net.mt32.expoll.entities.Users
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory{
    fun init(){
        val jdbcUrl = "jdbc:mariadb://${config.database.host}:${config.database.port}/expoll"

        val database = Database.connect(jdbcUrl, user= "root", password = "password")

        transaction {
            SchemaUtils.createDatabase("expoll")
            //SchemaUtils.createMissingTablesAndColumns(Users)
            val select = Users.selectAll()
            select.forEach{
                println(it[Users.id].toString() + " " + it[Users.username])
            }
            /*Users.insert {
                it[username] = "Testuiaskojfs"
            }*/
        }
    }
}

const val UUIDLength = 36

abstract class DatabaseEntity{
    abstract fun save()

    open fun saveRecursive(){}
}
