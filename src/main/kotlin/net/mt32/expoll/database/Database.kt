package net.mt32.expoll.database

import net.mt32.expoll.config
import net.mt32.expoll.database.transform.dateToTimestamp
import net.mt32.expoll.database.transform.dropAllForeignKeys
import net.mt32.expoll.entities.Session
import net.mt32.expoll.entities.Users
import net.mt32.expoll.entities.Vote
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.ResultSet

object DatabaseFactory {
    fun init() {
        val jdbcUrl = "jdbc:mariadb://${config.database.host}:${config.database.port}/expoll"

        val database = Database.connect(jdbcUrl, user = "root", password = "password")

        transaction {
            SchemaUtils.createDatabase("expoll")
            Transformer.transformTables()
            SchemaUtils.createMissingTablesAndColumns(Vote, Session)
            //SchemaUtils.createMissingTablesAndColumns(Users)
            val select = Users.selectAll()
            select.forEach {
                println(it[Users.id].toString() + " " + it[Users.username])
            }
            /*Users.insert {
                it[username] = "Testuiaskojfs"
            }*/
        }
    }

    fun <T : Any> runRawSQL(sql: String, transform: (ResultSet) -> T): T? {
        return transaction {
            return@transaction TransactionManager.current().exec(sql) {
                return@exec transform(it)
            }
        }
    }
}

object Transformer {
    /**
     * Transform tables and columns to convert to new "standard",
     * function is called before tables may not exist,
     * check for that before executing your alterations
     */
    fun transformTables() {
        dropAllForeignKeys()
        dateToTimestamp()
    }

    /**
     * Get columnname(index 0) and type(index 1) of a table
     */
    fun getTableColumns(table: String): Array<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        DatabaseFactory.runRawSQL("SELECT COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '${table}';") {
            while (it.next()) {
                list.add(Pair(it.getString(1), it.getString(2)))
            }
        }
        return list.toTypedArray()
    }

    /**
     * get column type
     */
    fun getColumnType(table: String, column: String): String? {
        var type: String? = null
        DatabaseFactory.runRawSQL("SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '${table}' AND COLUMN_NAME = '${column}';") {
            if (it.next()) {
                type = it.getString(1)
            }
        }
        return type
    }

    fun columnExists(table: String, column: String): Boolean{
        return getColumnType(table, column) != null
    }

    /**
     * add a new column
     * @return true on success, false otherwise
     */
    fun addColumn(table: String, columnName: String, type: String): Boolean {
        return DatabaseFactory.runRawSQL("ALTER TABLE $table ADD $columnName ${type};") {
            return@runRawSQL it.next()
        } ?: false
    }

    /**
     * drop a table
     */
    fun dropColumn(table: String, column: String): Boolean {
        return DatabaseFactory.runRawSQL("ALTER TABLE ${table} DROP COLUMN ${column};") {
            return@runRawSQL it.next()
        } ?: false
    }

    /**
     * Check if table exists
     */
    fun tableExists(table: String): Boolean {
        return DatabaseFactory.runRawSQL("SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '${table}';") {
            return@runRawSQL it.next()
        } ?: false
    }
}

/** Length of a uuid string*/
const val UUIDLength = 36

abstract class DatabaseEntity {

    /**
     * Save current entity to database
     */
    abstract fun save()

    /**
     * OPTIONALLY IMPLEMENTED
     * save current Entity and child entities
     */
    open fun saveRecursive() {}
}
