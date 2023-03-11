package net.mt32.expoll.helper

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Index
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager


// source from https://github.com/JetBrains/Exposed/issues/167#issuecomment-514558435
class UpsertStatement<Key : Any> :
    InsertStatement<Key> {

    private val indexName: String
    private val indexColumns: List<Column<*>>
    private val index: Boolean

    constructor(table: Table, conflictColumn: Column<*>? = null, conflictIndex: Index? = null) : super(table, false) {
        when {
            conflictIndex != null -> {
                index = true
                indexName = conflictIndex.indexName
                indexColumns = conflictIndex.columns
            }

            conflictColumn != null -> {
                index = false
                indexName = conflictColumn.name
                indexColumns = listOf(conflictColumn)
            }

            else -> throw IllegalArgumentException()
        }
    }

    constructor(table: Table, conflictColumns: List<Column<*>>? = null, conflictIndex: Index? = null) : super(
        table,
        false
    ) {
        when {
            conflictIndex != null -> {
                index = true
                indexName = conflictIndex.indexName
                indexColumns = conflictIndex.columns
            }

            conflictColumns != null -> {
                index = false
                indexName = ""
                indexColumns = conflictColumns
            }

            else -> throw IllegalArgumentException()
        }
    }

    override fun prepareSQL(transaction: Transaction) = buildString {
        append(super.prepareSQL(transaction))

        val dialect = transaction.db.vendor
        if (dialect == "postgresql") {
            if (index) {
                append(" ON CONFLICT ON CONSTRAINT ")
                append(indexName)
            } else {
                append(" ON CONFLICT(")
                append(indexName)
                append(")")
            }
            append(" DO UPDATE SET ")

            values.keys.filter { it !in indexColumns }
                .joinTo(this) { "${transaction.identity(it)}=EXCLUDED.${transaction.identity(it)}" }

        } else {

            append(" ON DUPLICATE KEY UPDATE ")
            values.keys.filter { it !in indexColumns }
                .joinTo(this) { "${transaction.identity(it)}=VALUES(${transaction.identity(it)})" }

        }
    }

}

inline fun <T : Table> T.upsert(
    conflictColumn: Column<*>? = null,
    conflictIndex: Index? = null,
    body: T.(UpsertStatement<Number>) -> Unit
) =
    UpsertStatement<Number>(this, conflictColumn, conflictIndex).apply {
        body(this)
        execute(TransactionManager.current())
    }

inline fun <T : Table> T.upsert(
    vararg
    conflictColumns: Column<*>,
    conflictIndex: Index? = null,
    body: T.(UpsertStatement<Number>) -> Unit
) =
    UpsertStatement<Number>(this, conflictColumns.toList(), conflictIndex).apply {
        body(this)
        execute(TransactionManager.current())
    }