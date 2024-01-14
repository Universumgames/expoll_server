package net.mt32.expoll.database.transform

import net.mt32.expoll.database.DatabaseFactory
import net.mt32.expoll.database.Transformer
import net.mt32.expoll.entities.Poll

fun Transformer.addPrivateVotingOption() {
    if (columnExists(Poll.tableName, Poll.privateVoting.name))
        return

    addColumn(Poll.tableName, Poll.privateVoting.name, "BOOLEAN")
    DatabaseFactory.runRawSQL("UPDATE ${Poll.tableName} SET ${Poll.privateVoting.name}=FALSE where ${Poll.privateVoting.name} is NULL") {}
}