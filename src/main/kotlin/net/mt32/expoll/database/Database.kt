package net.mt32.expoll.database

import net.mt32.expoll.config
import net.mt32.expoll.database.transform.*
import net.mt32.expoll.entities.*
import net.mt32.expoll.entities.notifications.APNDevice
import net.mt32.expoll.entities.notifications.WebNotificationDevice
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.ResultSet

object DatabaseFactory {

    var db: Database? = null
    private var connectionTries = 0

    /**
     * Initialize database connection, create scheme and missing tables
     */
    fun init() {

        if(connectionTries > 10){
            println("Database connection failed too many times ($connectionTries tries), aborting...")
            throw Exception("Database connection failed")
        }

        val jdbcUrl = "jdbc:mariadb://${config.database.host}:${config.database.port}/expoll"

        println("Connecting to database...")

        db = Database.connect(jdbcUrl, user = "root", password = "password")

        if (!connected) {
            connectionTries++
            println("Connection failed ($connectionTries), retrying in 5 seconds...")
            Thread.sleep(5000)
            init()
            return
        }
        println("Connected to database")

        transaction {
            SchemaUtils.createDatabase("expoll")
            if(Transformer.tableExists(User.tableName)) // if user table exists, all other tables exist
                try{
                    Transformer.transformTables()
                    println("Tables transformed")
                } catch (e: Exception){
                    println("Error transforming tables: ${e.message}")
                }
            SchemaUtils.createMissingTablesAndColumns(
                APNDevice,
                AppAttest,
                Authenticator,
                Challenge,
                DeleteConfirmation,
                MailRule,
                PollUserNote,
                NotificationPreferences,
                OIDCUserData,
                Poll,
                PollOptionString,
                PollOptionDate,
                PollOptionDateTime,
                OTP,
                Session,
                User,
                UserDeletionConfirmation,
                Vote,
                WebNotificationDevice,
                UserDeletionQueue
            )
            println("Database initialized")
        }
    }

    /**
     * run a raw sql statement
     * WARNING not all statements return something, so the function may not be called
     */
    fun <T : Any> runRawSQL(sql: String, transform: (ResultSet) -> T): T? {
        return transaction {
            return@transaction TransactionManager.current().exec(sql) {
                return@exec transform(it)
            }
        }
    }

    val connected: Boolean
        get() {
            return try {
                runRawSQL("SELECT 1;") {
                    return@runRawSQL it.next()
                } ?: false
            } catch (e: Exception) {
                false
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
        dropUnnecessaryColumns()
        addUserCreationColumn()
        linkAPNDeviceToSession()
        addPollJoinedTimestampColumn()
        addHiddenInListProperty()
        addMaxPollsOwnedColumn()
        addVoteChangeDetailedNotificationSetting()
        addPrivateVotingOption()
        addUserLastLogin()

        removeGhostVotes() // always last
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

    /**
     * Check if a column in a table exists
     * @param table the table name
     * @param column the column to check for
     * @return true if column exists in table, false otherwise
     */
    fun columnExists(table: String, column: String): Boolean {
        return getColumnType(table, column) != null
    }

    /**
     * add a new column
     * @return true on success, false otherwise
     */
    fun addColumn(table: String, columnName: String, type: String): Boolean {
        DatabaseFactory.runRawSQL("ALTER TABLE $table ADD $columnName ${type};") {
            // not called
        }
        return true
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

interface IDatabaseEntity {
    fun save(): Boolean
    fun delete(): Boolean
}

abstract class DatabaseEntity : IDatabaseEntity {

    /**
     * Save current entity to database
     */
    abstract override fun save(): Boolean

    /**
     * Delete the current entity from the database and all child objects
     */
    abstract override fun delete(): Boolean

    open fun saveConsecutive(): Boolean {
        return false
    }
}
