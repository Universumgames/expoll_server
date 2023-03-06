package net.mt32.expoll.database.transform

import net.mt32.expoll.database.DatabaseFactory
import net.mt32.expoll.database.Transformer
import java.util.*

/**
 * Transform all tables to use unix timestamps instead of date/datetime
 */
fun Transformer.dateToTimestamp(){
    sessionDateToTimestamp()
}

// Session
internal fun sessionDateToTimestamp(){
    if(!Transformer.tableExists("session"))
        return
    val expirationIsDateTime = Transformer.getColumnType("session", "expiration")
    if(expirationIsDateTime == null || !expirationIsDateTime.lowercase(Locale.getDefault()).contains("date"))
        return
    if(Transformer.columnExists("session", "expirationTimestamp"))
        return
    if(!Transformer.addColumn("session", "expirationTimestamp", "BIGINT"))
        throw Error("Couldn't create new column for session")
    /*val sessions = DatabaseFactory.runRawSQL("SELECT id, expiration FROM session;")
    while(sessions?.next() == true){
        val update = DatabaseFactory.runRawSQL("UPDATE session SET ")
    }*/
    DatabaseFactory.runRawSQL("UPDATE session SET expirationTimestamp=UNIX_TIMESTAMP(expiration);"){}
    // TODO remove comment
    //val dropColumn = Transformer.dropColumn("session", "expiration")
}