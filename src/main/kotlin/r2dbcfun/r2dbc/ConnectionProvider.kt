package r2dbcfun.r2dbc

import io.r2dbc.spi.Clob
import io.r2dbc.spi.R2dbcException
import io.r2dbc.spi.Row
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import org.reactivestreams.Publisher
import r2dbcfun.RepositoryException
import r2dbcfun.executeInsert
import r2dbcfun.transaction.transaction

class ConnectionProvider(val r2dbcConnection: R2dbcConnection) {
    constructor(connection: io.r2dbc.spi.Connection) : this(R2dbcConnection(connection))

    suspend fun <T> transaction(function: suspend () -> T): T = transaction(r2dbcConnection.connection, function)
}

interface DatabaseConnection {
    suspend fun executeSelect(
        parameterValues: Sequence<Any>,
        sql: String
    ): DBResult

    fun beginTransaction(): Publisher<Void>
    fun commitTransaction(): Publisher<Void>
    fun createStatement(sql: String): Statement
    fun rollbackTransaction(): Publisher<Void>
}

interface Statement {
    fun bind(idx: Int, property: Any): Statement
    fun bind(field: String, property: Any): Statement
    suspend fun execute(): DBResult
    fun bindNull(index: Int, dbClass: Class<out Any>): Statement
    suspend fun executeInsert(): Long
}

class R2dbcConnection(val connection: io.r2dbc.spi.Connection) : DatabaseConnection {
    override suspend fun executeSelect(
        parameterValues: Sequence<Any>,
        sql: String
    ): DBResult {
        val statement = try {
            parameterValues.foldIndexed(createStatement(sql))
            { idx, statement, property -> statement.bind(idx, property) }
        } catch (e: R2dbcException) {
            throw RepositoryException("error creating statement for sql:$sql", e)
        }
        return try {
            statement.execute()
        } catch (e: R2dbcException) {
            throw RepositoryException("error executing select: $sql", e)
        }
    }

    override fun beginTransaction(): Publisher<Void> {
        return connection.beginTransaction()
    }

    override fun commitTransaction(): Publisher<Void> {
        return connection.commitTransaction()
    }

    override fun createStatement(sql: String): Statement {
        return R2dbcStatement(connection.createStatement(sql))
    }

    override fun rollbackTransaction(): Publisher<Void> {
        return connection.rollbackTransaction()
    }
}

class R2dbcStatement(val statement: io.r2dbc.spi.Statement) : Statement {
    override fun bind(idx: Int, property: Any): Statement {
        statement.bind(idx, property)
        return this
    }

    override fun bind(field: String, property: Any): Statement {
        statement.bind(field, property)
        return this
    }

    override suspend fun execute(): DBResult {
        return R2dbcResult(statement.execute().awaitSingle())
    }

    override fun bindNull(index: Int, dbClass: Class<out Any>): Statement {
        statement.bindNull(index, dbClass)
        return this
    }

    override suspend fun executeInsert(): Long {
        return statement.executeInsert()
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
    fun <T> get(key: String, type: Class<T>): T?
}

class R2dbcRow(private val row: Row) : DBRow {
    override fun getLazy(key: String): LazyResult<Any?> {
        val value = row.get(key)
        return if (value is Clob) LazyResult { resolveClob(value) } else LazyResult { value }
    }

    override fun <T> get(key: String, type: Class<T>): T? = row.get(key, type)

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
