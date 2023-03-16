package net.mt32.expoll.database.transform

import net.mt32.expoll.database.DatabaseFactory
import net.mt32.expoll.database.Transformer
import net.mt32.expoll.entities.User
import net.mt32.expoll.helper.UnixTimestamp

fun Transformer.addUserCreationColumn() {
    if(!tableExists(User.tableName)) return
    if(!columnExists(User.tableName, User.created.name)){
        addColumn(User.tableName, User.created.name, "BIGINT")
    }
    DatabaseFactory.runRawSQL("UPDATE ${User.tableName} SET ${User.created.name}=${UnixTimestamp.now().secondsSince1970} WHERE ${User.created.name} is NULL") {}
}