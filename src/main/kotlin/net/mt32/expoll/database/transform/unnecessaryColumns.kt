package net.mt32.expoll.database.transform

import net.mt32.expoll.database.DatabaseFactory
import net.mt32.expoll.database.Transformer
import net.mt32.expoll.entities.PollUserNote

fun Transformer.dropUnnecessaryColumns() {
    dropUserNoteIDColumn()
}

fun dropUserNoteIDColumn() {
    if (Transformer.columnExists(PollUserNote.tableName, "id")) {
        Transformer.dropColumn(PollUserNote.tableName, "id")
        DatabaseFactory.runRawSQL("ALTER TABLE ${PollUserNote.tableName} ADD CONSTRAINT PK_NOTE PRIMARY KEY (userId, pollId);") {}
    }
}