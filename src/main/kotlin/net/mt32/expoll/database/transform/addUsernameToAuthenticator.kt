package net.mt32.expoll.database.transform

import net.mt32.expoll.database.DatabaseFactory
import net.mt32.expoll.database.Transformer
import net.mt32.expoll.entities.Authenticator
import net.mt32.expoll.entities.User

fun Transformer.addUsernameToAuthenticator() {
    if (columnExists(Authenticator.tableName, Authenticator.usedUsername.name))
        return

    addColumn(Authenticator.tableName, Authenticator.usedUsername.name, Authenticator.usedUsername.columnType.sqlType())
    DatabaseFactory.runRawSQL("UPDATE ${Authenticator.tableName} SET ${Authenticator.usedUsername.name}=(" +
            "SELECT username FROM ${User.tableName} user WHERE user.id=${Authenticator.tableName}.userId" +
            ")") {}
}