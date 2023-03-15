package net.mt32.expoll.database.transform

import net.mt32.expoll.database.DatabaseFactory
import net.mt32.expoll.database.Transformer

fun Transformer.dropAllForeignKeys(){
    DatabaseFactory.runRawSQL("SELECT concat('ALTER TABLE ', TABLE_NAME, ' DROP FOREIGN KEY ', CONSTRAINT_NAME, ';')  FROM information_schema.key_column_usage  WHERE CONSTRAINT_SCHEMA = 'expoll'  AND referenced_table_name IS NOT NULL;"){
        while(it.next()){
            DatabaseFactory.runRawSQL(it.getString(1)){}
        }
    }
}