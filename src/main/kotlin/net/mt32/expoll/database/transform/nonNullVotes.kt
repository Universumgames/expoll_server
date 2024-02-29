package net.mt32.expoll.database.transform

import net.mt32.expoll.VoteValue
import net.mt32.expoll.database.DatabaseFactory
import net.mt32.expoll.database.Transformer
import net.mt32.expoll.entities.Poll

fun Transformer.nonNullDefaultVotes() {
    // check if column Poll.defaultVote can be null, set default to UNKNOWN if it is and change column to NOT NULL
    // transformation to NOT NULL is handled by exposed
    DatabaseFactory.runRawSQL("UPDATE ${Poll.tableName} SET ${Poll.defaultVote.name}=${VoteValue.UNKNOWN.id} WHERE ${Poll.defaultVote.name} is NULL") {}
    //DatabaseFactory.runRawSQL("ALTER TABLE ${Poll.tableName} ALTER COLUMN ${Poll.defaultVote.name} SET NOT NULL") { }
}