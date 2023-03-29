package net.mt32.expoll.database.transform

import net.mt32.expoll.database.DatabaseFactory
import net.mt32.expoll.database.Transformer
import net.mt32.expoll.entities.APNDevice

fun Transformer.linkAPNDeviceToSession() {
    if (columnExists(APNDevice.tableName, APNDevice.sessionNonce.name))
        return

    addColumn(APNDevice.tableName, APNDevice.sessionNonce.name, "LONG")
    DatabaseFactory.runRawSQL("UPDATE ${APNDevice.tableName} SET ${APNDevice.sessionNonce.name}=0 where ${APNDevice.sessionNonce.name} is NULL") {}
}