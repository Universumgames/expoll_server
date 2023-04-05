package net.mt32.expoll.analytics

import kotlinx.serialization.Serializable
import net.mt32.expoll.entities.*
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class DatabaseCounts(
    val userCount: Long,
    val voteCount: Long,
    val pollCount: Long,
    val authDeviceCount: Long,
    val sessionCount: Long,
    val appSessionCount: Long
)

fun AnalyticsStorage.getCounts(): DatabaseCounts{
    return transaction {
        val userCount = User.selectAll().count()
        val voteCount = Vote.selectAll().count()
        val pollCount = Poll.selectAll().count()
        val authDeviceCount = Authenticator.selectAll().count()
        val sessionCount = Session.selectAll().count()
        val appSessionCount = Session.select { Session.userAgent eq "ios App" }.count()
        return@transaction DatabaseCounts(
            userCount,
            voteCount,
            pollCount,
            authDeviceCount,
            sessionCount,
            appSessionCount
        )
    }
}