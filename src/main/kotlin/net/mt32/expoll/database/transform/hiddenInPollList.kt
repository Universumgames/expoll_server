package net.mt32.expoll.database.transform

import net.mt32.expoll.database.DatabaseFactory
import net.mt32.expoll.database.Transformer
import net.mt32.expoll.entities.UserPolls

fun Transformer.addHiddenInListProperty(){
    if(!tableExists(UserPolls.tableName)) return
    if(columnExists(UserPolls.tableName, UserPolls.listHidden.name)) return

    addColumn(UserPolls.tableName, UserPolls.listHidden.name, "BOOLEAN")
    DatabaseFactory.runRawSQL("UPDATE ${UserPolls.tableName} SET ${UserPolls.listHidden.name}=FALSE;"){}
}