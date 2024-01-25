package net.mt32.expoll.analytics

import kotlinx.serialization.Serializable
import net.mt32.expoll.database.DatabaseFactory
import net.mt32.expoll.entities.*
import net.mt32.expoll.entities.notifications.APNDevice
import net.mt32.expoll.serializable.request.Platform
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class DatabaseCounts(
    val userCount: Long,
    val voteCount: Long,
    val pollCount: Long,
    val authDeviceCount: Long,
    val sessionCount: Long,
    val appSessionCount: Long,
    val notificationDeviceCount: Long,
    val deletedUserCount: Long,
    val oidcConnections: Long,
    val totalDiskUsage: Long
)

fun AnalyticsStorage.getCounts(): DatabaseCounts {
    return transaction {
        val userCount = User.selectAll().count()
        val voteCount = Vote.selectAll().count()
        val pollCount = Poll.selectAll().count()
        val authDeviceCount = Authenticator.selectAll().count()
        val sessionCount = Session.selectAll().count()
        val appSessionCount = Session.selectAll()
            .where {
                (Session.platform eq Platform.IOS.name) or
                        (Session.platform eq Platform.ANDROID.name)
            }.count()
        val deletedUserCount = User.selectAll().where { User.deleted neq null}.count()
        val notificationDevices = APNDevice.selectAll().count()
        val oidcConnections = OIDCUserData.selectAll().count()
        var totalDiskUsage = -1L
        DatabaseFactory.runRawSQL("SELECT table_schema AS \"Database\", SUM(data_length + index_length) AS \"Size\" FROM information_schema.TABLES GROUP BY table_schema"){
            while(it.next()){
                val dbName = it.getString("Database")
                val size = it.getLong("Size")
                if(dbName == "expoll"){
                    totalDiskUsage = size
                }
            }
        }
        return@transaction DatabaseCounts(
            userCount,
            voteCount,
            pollCount,
            authDeviceCount,
            sessionCount,
            appSessionCount,
            notificationDevices,
            deletedUserCount,
            oidcConnections,
            totalDiskUsage
        )
    }
}