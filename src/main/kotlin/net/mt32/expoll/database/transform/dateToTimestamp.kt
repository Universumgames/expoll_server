package net.mt32.expoll.database.transform

import net.mt32.expoll.database.DatabaseFactory
import net.mt32.expoll.database.Transformer
import net.mt32.expoll.entities.*
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import java.util.*

/**
 * Transform all tables to use unix timestamps instead of date/datetime
 */
fun Transformer.dateToTimestamp() {
    sessionDateToTimestamp()
    pollOptionDateToTimestamp()
    apnDeviceDateToTimestamp()
    confirmationDateToTimestamp()
}

private fun changeDateColumnToTimestamp(
    table: Table,
    column: Column<*>,
    oldName: String,
    additionalTypeInfo: String = ""
) {
    if (!Transformer.tableExists(table.tableName))
        return
    val columnToChangeIsDate = Transformer.getColumnType(table.tableName, oldName)
    if (columnToChangeIsDate == null || !columnToChangeIsDate.lowercase(Locale.getDefault()).contains("date"))
        return
    if (!Transformer.columnExists(table.tableName, column.name)) {
        if (!Transformer.addColumn(table.tableName, column.name, "BIGINT $additionalTypeInfo"))
            throw Error("Couldn't create new column for ${table.tableName}.${column.name}")
        DatabaseFactory.runRawSQL("UPDATE ${table.tableName} SET ${column.name}=UNIX_TIMESTAMP(${oldName});") {}
    }

    // TODO remove comment
    //if (Transformer.columnExists(table.tableName, oldName))
    //    Transformer.dropColumn(table.tableName, oldName)
    // or change to computed column
    //Transformer.dropColumn(table.tableName, oldName)
    //Transformer.addColumn(table.tableName, oldName, "DATE GENERATED ALWAYS AS (FROM_UNIXTIME(${column.name}))")
}

private fun confirmationDateToTimestamp(){
    changeDateColumnToTimestamp(DeleteConfirmation.Companion, DeleteConfirmation.expirationTimestamp, "expiration")
}

private fun apnDeviceDateToTimestamp() {
    changeDateColumnToTimestamp(APNDevice.Companion, APNDevice.creationTimestamp, "creation")
    changeDateColumnToTimestamp(AppAttest.Companion, AppAttest.createdAtTimestamp, "createdAt")
}

private fun pollOptionDateToTimestamp() {
    changeDateColumnToTimestamp(
        PollOptionDateTime.Companion,
        PollOptionDateTime.dateTimeStartTimestamp,
        "dateTimeStart"
    )
    changeDateColumnToTimestamp(PollOptionDateTime.Companion, PollOptionDateTime.dateTimeEndTimestamp, "dateTimeEnd")

    changeDateColumnToTimestamp(PollOptionDate.Companion, PollOptionDate.dateStartTimestamp, "dateStart")
    changeDateColumnToTimestamp(PollOptionDate.Companion, PollOptionDate.dateEndTimestamp, "dateEnd")
}

// Session
private fun sessionDateToTimestamp() {
    changeDateColumnToTimestamp(Session.Companion, Session.expirationTimestamp, "expiration")
}