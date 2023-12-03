package net.mt32.expoll.database.transform

import net.mt32.expoll.database.DatabaseFactory
import net.mt32.expoll.database.Transformer
import net.mt32.expoll.entities.User

fun Transformer.addMaxPollsOwnedColumn() {
    if (columnExists(User.tableName, User.maxPollsOwned.name))
        return

    addColumn(User.tableName, User.maxPollsOwned.name, "BIGINT")
    DatabaseFactory.runRawSQL("UPDATE ${User.tableName} SET ${User.maxPollsOwned.name}=10 where ${User.maxPollsOwned.name} is NULL") {}
}