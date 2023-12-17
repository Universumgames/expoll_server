package net.mt32.expoll.database.transform

import net.mt32.expoll.database.DatabaseFactory
import net.mt32.expoll.database.Transformer
import net.mt32.expoll.entities.NotificationPreferences

fun Transformer.addVoteChangeDetailedNotificationSetting() {
    if(columnExists(NotificationPreferences.tableName, NotificationPreferences.voteChangeDetailed.name))
        return
    addColumn(NotificationPreferences.tableName, NotificationPreferences.voteChangeDetailed.name, "BOOLEAN")
    DatabaseFactory.runRawSQL("UPDATE ${NotificationPreferences.tableName} SET ${NotificationPreferences.voteChangeDetailed.name}=FALSE where ${NotificationPreferences.voteChangeDetailed.name} is NULL") {}
}