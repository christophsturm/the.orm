package r2dbcfun.dbio.vertx

import io.vertx.reactivex.sqlclient.Row
import io.vertx.reactivex.sqlclient.RowSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import r2dbcfun.dbio.DBResult
import r2dbcfun.dbio.DBRow

class VertxResult(private val rows: RowSet<Row>) : DBResult {
    override suspend fun rowsUpdated(): Int {
        return rows.rowCount()
    }

    override suspend fun <T : Any> map(mappingFunction: (t: DBRow) -> T): Flow<T> {
        val flow: Flow<Row> = rows.asFlow()
        return flow.map { mappingFunction(VertxRow(it)) }
    }

}
