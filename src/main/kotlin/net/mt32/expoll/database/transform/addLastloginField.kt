package net.mt32.expoll.database.transform

import net.mt32.expoll.database.DatabaseFactory
import net.mt32.expoll.database.Transformer
import net.mt32.expoll.entities.User
import net.mt32.expoll.commons.helper.UnixTimestamp

fun Transformer.addUserLastLogin(){
    if(columnExists(User.tableName, User.lastLogin.name))
        return

    addColumn(User.tableName, User.lastLogin.name, User.lastLogin.columnType.sqlType())
    DatabaseFactory.runRawSQL("UPDATE ${User.tableName} SET ${User.lastLogin.name}=${UnixTimestamp.now().toDB()} where ${User.lastLogin.name} is NULL") {}
}