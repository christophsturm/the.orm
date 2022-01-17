package io.the.orm.dbio.r2dbc

import io.the.orm.dbio.DBIOException
import io.the.orm.dbio.DBResult
import io.the.orm.dbio.DBRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import org.reactivestreams.Publisher

class R2dbcResult(private val result: io.r2dbc.spi.Result) : DBResult {
    override suspend fun rowsUpdated(): Int = result.rowsUpdated.awaitSingle()

    override suspend fun <T : Any> map(mappingFunction: (t: DBRow) -> T): Flow<T> {
        val mapped: Publisher<T> = try {
            result.map { row, _ ->
                try {
                    mappingFunction(R2dbcRow(row))
                } catch (e: Exception) {
                    throw DBIOException("error in mapping function", e)
                }
            }
        } catch (e: Exception) {
            throw DBIOException("error in map", e)
        }
        return mapped.asFlow()
    }
}
