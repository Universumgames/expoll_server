package net.mt32.expoll.entities.interconnect

import net.mt32.expoll.database.UUIDLength
import net.mt32.expoll.commons.helper.UnixTimestamp
import net.mt32.expoll.commons.helper.toUnixTimestampAsSecondsSince1970
import net.mt32.expoll.commons.helper.toUnixTimestampFromDB
import net.mt32.expoll.commons.tPollID
import net.mt32.expoll.commons.tUserID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

data class PollJoinTimestamp(val userID: tUserID, val joinTimestamp: UnixTimestamp) {

}

object UserPolls : Table("user_polls_poll") {
    val userID = varchar("userId", UUIDLength)
    val pollID = varchar("pollId", UUIDLength)
    val joinedTimestamp = long("joinedTimestamp")
    val listHidden = bool("listHidden")

    override val primaryKey = PrimaryKey(userID, pollID)

    fun connectionExists(userID: tUserID, pollID: tPollID): Boolean {
        return transaction {
            return@transaction !UserPolls.selectAll().where {
                (UserPolls.userID eq userID) and (UserPolls.pollID eq pollID)
            }.empty()
        }
    }

    fun addConnection(
        userID: tUserID, pollID: tPollID, joinTimestamp: UnixTimestamp = UnixTimestamp.now(), hide: Boolean = false
    ) {
        if (connectionExists(userID, pollID)) return
        transaction {
            UserPolls.insert {
                it[UserPolls.pollID] = pollID
                it[UserPolls.userID] = userID
                it[joinedTimestamp] = joinTimestamp.toDB()
                it[listHidden] = hide
            }
        }
    }

    fun removeConnection(userID: tUserID, pollID: tPollID) {
        transaction {
            UserPolls.deleteWhere {
                (UserPolls.userID eq userID) and (UserPolls.pollID eq pollID)
            }
        }
    }

    fun userIDs(pollID: tPollID): List<tUserID> {
        return transaction {
            return@transaction UserPolls.selectAll().where {
                UserPolls.pollID eq pollID
            }.toList().map { it[userID] }
        }
    }

    fun userCount(pollID: tPollID): Int {
        return userIDs(pollID).size
    }

    fun joinedTimestamps(pollID: tPollID): List<PollJoinTimestamp> {
        return transaction {
            return@transaction UserPolls.selectAll().where {
                UserPolls.pollID eq pollID
            }.toList()
                .map { PollJoinTimestamp(it[userID], it[joinedTimestamp].toUnixTimestampFromDB()) }
        }
    }

    fun hideFromListForUser(pollID: tPollID, userID: tUserID, hidden: Boolean = true) {
        val joinTimestamp = transaction {
            UserPolls.selectAll().where { (UserPolls.pollID eq pollID) and (UserPolls.userID eq userID) }.firstOrNull()
                ?.get(joinedTimestamp)
        }?.toUnixTimestampAsSecondsSince1970() ?: UnixTimestamp.now()
        removeConnection(userID, pollID)
        addConnection(userID, pollID, joinTimestamp, hidden)
    }

    fun getHidden(pollID: tPollID, userID: tUserID): Boolean {
        return transaction {
            val rs = UserPolls.selectAll().where { (UserPolls.pollID eq pollID) and (UserPolls.userID eq userID) }
                .firstOrNull()
            val hidden = rs?.getOrNull(listHidden)
            return@transaction hidden ?: false
        }
    }
}