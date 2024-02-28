package net.mt32.expoll.database.transform

import net.mt32.expoll.database.DatabaseFactory
import net.mt32.expoll.database.Transformer
import net.mt32.expoll.entities.interconnect.UserPolls
import net.mt32.expoll.helper.UnixTimestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun Transformer.addPollJoinedTimestampColumn() {
    if(!tableExists(UserPolls.tableName)) return
    if(!columnExists(UserPolls.tableName, UserPolls.joinedTimestamp.name)){
        addColumn(UserPolls.tableName, UserPolls.joinedTimestamp.name, "BIGINT")
        val users = transaction { UserPolls.selectAll().map { it[UserPolls.userID] } }.sortedBy { it }
        var timestamp = UnixTimestamp.now().toDB()
        users.forEach {
            DatabaseFactory.runRawSQL(
                "UPDATE ${UserPolls.tableName} " +
                        "SET ${UserPolls.joinedTimestamp.name}=${timestamp++} " +
                        "WHERE ${UserPolls.joinedTimestamp.name} is NULL " +
                        "AND ${UserPolls.userID.name}='$it';"
            ) {
            }
        }
    }
    return
}