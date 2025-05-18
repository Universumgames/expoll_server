package net.mt32.expoll.database.transform

import net.mt32.expoll.database.DatabaseFactory
import net.mt32.expoll.database.Transformer
import net.mt32.expoll.entities.Poll
import net.mt32.expoll.entities.PollOptionDate
import net.mt32.expoll.entities.PollOptionDateTime

fun Transformer.addTimezoneColumn() {
    if(!columnExists(PollOptionDate.tableName, PollOptionDate.timeZone.name)){
        addColumn(PollOptionDate.tableName, PollOptionDate.timeZone.name, PollOptionDate.timeZone.columnType.sqlType())
        DatabaseFactory.runRawSQL("UPDATE ${PollOptionDate.tableName} SET ${PollOptionDate.timeZone.name} = 'Europe/Berlin' WHERE ${PollOptionDate.timeZone.name} IS NULL"){}
    }
    if(!columnExists(PollOptionDateTime.tableName, PollOptionDateTime.timeZone.name)){
        addColumn(PollOptionDateTime.tableName, PollOptionDateTime.timeZone.name, PollOptionDateTime.timeZone.columnType.sqlType())
        DatabaseFactory.runRawSQL("UPDATE ${PollOptionDateTime.tableName} SET ${PollOptionDateTime.timeZone.name} = 'Europe/Berlin' WHERE ${PollOptionDateTime.timeZone.name} IS NULL"){}
    }
    if(!columnExists(Poll.tableName, Poll.useUTC.name)){
        addColumn(Poll.tableName, Poll.useUTC.name, Poll.useUTC.columnType.sqlType())
        DatabaseFactory.runRawSQL("UPDATE ${Poll.tableName} SET ${Poll.useUTC.name} = true WHERE ${Poll.useUTC.name} IS NULL"){}
    }
}