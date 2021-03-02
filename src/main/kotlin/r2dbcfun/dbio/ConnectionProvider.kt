package r2dbcfun.dbio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.single

class TransactionalConnectionProvider(val connectionFactory: ConnectionFactory) : ConnectionProvider {
    override suspend fun <T> transaction(function: suspend (ConnectionProvider) -> T): T {
        val connection = connectionFactory.getConnection()
        connection.beginTransaction()
        val result = try {
            function(FixedConnectionProvider(connection))
        } catch (e: Exception) {
            connection.rollbackTransaction()
            throw e
        }
        connection.commitTransaction()
        connection.close()
        return result
    }

    override suspend fun <T> withConnection(function: suspend (DBConnection) -> T): T {
        val connection = connectionFactory.getConnection()
        return try {
            function(connection)
        } catch (e: Exception) {
            connection.rollbackTransaction()
            throw e
        } finally {
            connection.close()
        }

    }
}

class FixedConnectionProvider(val connection: DBConnection) : ConnectionProvider {
    override suspend fun <T> transaction(function: suspend (ConnectionProvider) -> T): T = function(this)
    override suspend fun <T> withConnection(function: suspend (DBConnection) -> T): T = function(connection)
}

interface ConnectionProvider {
    suspend fun <T> transaction(function: suspend (ConnectionProvider) -> T): T

    suspend fun <T> withConnection(function: suspend (DBConnection) -> T): T
}

interface DBConnection {
    fun createStatement(sql: String): Statement
    fun createInsertStatement(sql: String): Statement
    suspend fun beginTransaction()
    suspend fun commitTransaction()
    suspend fun rollbackTransaction()
    suspend fun close()
}

suspend fun DBConnection.executeSelect(
    parameterValues: Sequence<Any>,
    sql: String
): DBResult = createStatement(sql).execute(listOf(), parameterValues)

interface Statement {
    suspend fun execute(types: List<Class<*>>, values: Sequence<Any?>): DBResult
}

interface DBResult {

    suspend fun rowsUpdated(): Int
    suspend fun <T : Any> map(mappingFunction: (t: DBRow) -> T): Flow<T>
    suspend fun getId(): Long {
        return this.map { row -> row.get("id", java.lang.Long::class.java)!!.toLong() }.single()

    }
}

interface DBRow {
    fun getLazy(key: String): LazyResult<Any?>
    fun <T> get(key: String, type: Class<T>): T?
}

class LazyResult<T>(val get: suspend () -> T) {
    suspend fun resolve() = get()
}
