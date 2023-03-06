package net.mt32.expoll.database.transform

import net.mt32.expoll.database.DatabaseFactory
import net.mt32.expoll.database.Transformer
import java.util.*

/**
 * Transform all tables to use unix timestamps instead of date/datetime
 */
fun Transformer.dateToTimestamp() {
    sessionDateToTimestamp()
    pollOptionDateToTimestamp()
}

internal fun pollOptionDateToTimestamp() {
    if (Transformer.tableExists("poll_option_date_time")) {
        val columnType = Transformer.getColumnType("poll_option_date_time", "dateTimeStart")
        if (columnType != null && columnType.lowercase().contains("date")) {
            if (!Transformer.columnExists("poll_option_date_time", "dateTimeStartTimestamp")) {
                // start time
                if (!Transformer.addColumn("poll_option_date_time", "dateTimeStartTimestamp", "BIGINT"))
                    throw Error("Couldn't create new column for poll_option_date_time")
                DatabaseFactory.runRawSQL("UPDATE poll_option_date_time SET dateTimeStartTimestamp=UNIX_TIMESTAMP(dateTimeStart);") {}
                //Transformer.dropColumn("poll_option_date_time", "dateTimeStart")
            }
            if (!Transformer.columnExists("poll_option_date_time", "dateTimeEndTimestamp")) {
                //end time
                if (!Transformer.addColumn("poll_option_date_time", "dateTimeEndTimestamp", "BIGINT NULL DEFAULT NULL"))
                    throw Error("Couldn't create new column for poll_option_date_time")
                DatabaseFactory.runRawSQL("UPDATE poll_option_date_time SET dateTimeEndTimestamp=UNIX_TIMESTAMP(dateTimeEnd);") {}
                //Transformer.dropColumn("poll_option_date_time", "dateTimeEnd")
            }
        }
    }
    if (Transformer.tableExists("poll_option_date")) {
        val columnType = Transformer.getColumnType("poll_option_date", "dateStart")
        if (columnType != null && columnType.lowercase().contains("date")) {
            if (!Transformer.columnExists("poll_option_date", "dateStartTimestamp")) {
                // start time
                if (!Transformer.addColumn("poll_option_date", "dateStartTimestamp", "BIGINT"))
                    throw Error("Couldn't create new column for poll_option_date")
                DatabaseFactory.runRawSQL("UPDATE poll_option_date SET dateStartTimestamp=UNIX_TIMESTAMP(dateStart);") {}
                //Transformer.dropColumn("poll_option_date", "dateStart")
            }
            if (!Transformer.columnExists("poll_option_date", "dateEndTimestamp")) {
                //end time
                if (!Transformer.addColumn("poll_option_date", "dateEndTimestamp", "BIGINT NULL DEFAULT NULL"))
                    throw Error("Couldn't create new column for poll_option_date")
                DatabaseFactory.runRawSQL("UPDATE poll_option_date SET dateEndTimestamp=UNIX_TIMESTAMP(dateEnd);") {}
                //Transformer.dropColumn("poll_option_date", "dateEnd")
            }
        }
    }
}

// Session
internal fun sessionDateToTimestamp() {
    if (!Transformer.tableExists("session"))
        return
    val expirationIsDateTime = Transformer.getColumnType("session", "expiration")
    if (expirationIsDateTime == null || !expirationIsDateTime.lowercase(Locale.getDefault()).contains("date"))
        return
    if (Transformer.columnExists("session", "expirationTimestamp"))
        return
    if (!Transformer.addColumn("session", "expirationTimestamp", "BIGINT"))
        throw Error("Couldn't create new column for session")
    /*val sessions = DatabaseFactory.runRawSQL("SELECT id, expiration FROM session;")
    while(sessions?.next() == true){
        val update = DatabaseFactory.runRawSQL("UPDATE session SET ")
    }*/
    DatabaseFactory.runRawSQL("UPDATE session SET expirationTimestamp=UNIX_TIMESTAMP(expiration);") {}
    // TODO remove comment
    //val dropColumn = Transformer.dropColumn("session", "expiration")
}