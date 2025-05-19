package net.mt32.expoll.database.transform

import net.mt32.expoll.database.Transformer
import net.mt32.expoll.entities.User

fun Transformer.addPersonalizedDateRequestCountColumn() {
    if(!columnExists(User.tableName, User.personalDataRequestCount.name)){
        addColumn(
            User.tableName,
            User.personalDataRequestCount.name,
            User.personalDataRequestCount.columnType.sqlType()
        )
    }
}