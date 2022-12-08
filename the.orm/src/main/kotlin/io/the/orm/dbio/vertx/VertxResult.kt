package io.the.orm.dbio.vertx

import io.the.orm.dbio.DBResult
import io.the.orm.dbio.DBRow
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map

class VertxResult(private val rows: RowSet<Row>) : DBResult {
    override suspend fun rowsUpdated(): Long = rows.rowCount().toLong()

    override suspend fun <T : Any> map(mappingFunction: (t: DBRow) -> T): Flow<T> {
        val flow: Flow<Row> = rows.asFlow()
        return flow.map { mappingFunction(VertxRow(it)) }
    }

    override fun asMapFlow(): Flow<Map<String, Any?>> {
        val names = rows.columnsNames().map { it.lowercase() }
        return rows.asFlow().map { row -> names.withIndex().associateTo(HashMap()) {
                (index, name) -> name to row.get(Any::class.java, index)
        } }
    }
}
