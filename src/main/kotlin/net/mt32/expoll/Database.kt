package net.mt32.expoll

import org.jetbrains.exposed.sql.Database

object DatabaseFactory{
    fun init(){
        val driverClassName = "com.mysql.jdbc.Driver"
        val jdbcUrl = "jdbc:mysql://localhost/expoll?user=expoll&password=password"

        val database = Database.connect(jdbcUrl, driverClassName)

    }
}