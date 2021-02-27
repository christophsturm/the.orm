package r2dbcfun.r2dbc

import io.r2dbc.spi.Clob
import io.r2dbc.spi.R2dbcException
import io.r2dbc.spi.Row
import io.r2dbc.spi.Statement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import org.reactivestreams.Publisher
import r2dbcfun.RepositoryException
import r2dbcfun.transaction.transaction

class ConnectionProvider(val r2dbcConnection: R2dbcConnection) {
    constructor(connection: io.r2dbc.spi.Connection) : this(R2dbcConnection(connection))

    suspend fun <T> transaction(function: suspend () -> T): T = r2dbcConnection.connection.transaction(function)
}

interface DatabaseConnection {
    suspend fun executeSelect(
        parameterValues: Sequence<Any>,
        sql: String
    ): R2dbcResult

    fun beginTransaction(): Publisher<Void>
    fun commitTransaction(): Publisher<Void>
    fun createStatement(sql: String): Statement
    fun rollbackTransaction(): Publisher<Void>
}

class R2dbcConnection(val connection: io.r2dbc.spi.Connection) : DatabaseConnection {
    override suspend fun executeSelect(
        parameterValues: Sequence<Any>,
        sql: String
    ): R2dbcResult {
        val statement = try {
            parameterValues.foldIndexed(createStatement(sql))
            { idx, statement, property -> statement.bind(idx, property) }
        } catch (e: R2dbcException) {
            throw RepositoryException("error creating statement for sql:$sql", e)
        }
        return R2dbcResult(
            try {
                statement.execute().awaitSingle()
            } catch (e: R2dbcException) {
                throw RepositoryException("error executing select: $sql", e)
            }
        )
    }

    override fun beginTransaction(): Publisher<Void> {
        return connection.beginTransaction()
    }

    override fun commitTransaction(): Publisher<Void> {
        return connection.commitTransaction()
    }

    override fun createStatement(sql: String): Statement {
        return connection.createStatement(sql)
    }

    override fun rollbackTransaction(): Publisher<Void> {
        return connection.rollbackTransaction()
    }
}

interface DBResult {
    suspend fun rowsUpdated(): Int
    fun <T : Any> map(mappingFunction: (t: R2dbcRow) -> T): Flow<T>
}

class R2dbcResult(private val result: io.r2dbc.spi.Result) : DBResult {
    override suspend fun rowsUpdated(): Int = result.rowsUpdated.awaitSingle()

    override fun <T : Any> map(mappingFunction: (t: R2dbcRow) -> T): Flow<T> {
        return result.map { row, _ -> mappingFunction(R2dbcRow(row)) }.asFlow()
    }
}

interface DBRow {
    fun getLazy(key: String): LazyResult<Any?>
}

class R2dbcRow(private val row: Row) : DBRow {
    override fun getLazy(key: String): LazyResult<Any?> {
        val value = row.get(key)
        return if (value is Clob) LazyResult { resolveClob(value) } else LazyResult { value }
    }

    private suspend fun resolveClob(result: Clob): String {
        val sb = StringBuilder()
        result.stream()
            .asFlow()
            .collect { chunk -> @Suppress("BlockingMethodInNonBlockingContext") sb.append(chunk) }
        result.discard()
        return sb.toString()
    }

}

class LazyResult<T>(val get: suspend () -> T) {
    suspend fun resolve() = get()
}
