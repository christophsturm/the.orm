package r2dbcfun.dbio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.single

class TransactionalConnectionProvider(val DBConnectionFactory: DBConnectionFactory) : TransactionProvider {
    override suspend fun <T> transaction(function: suspend (ConnectionProvider) -> T): T {
        val connection = DBConnectionFactory.getConnection()
        val transaction = connection.beginTransaction()
        val result = try {
            function(FixedConnectionProvider(connection))
        } catch (e: Exception) {
            transaction.rollbackTransaction()
            throw e
        }
        transaction.commitTransaction()
        connection.close()
        return result
    }

    override suspend fun <T> withConnection(function: suspend (DBConnection) -> T): T {
        val connection = DBConnectionFactory.getConnection()
        return try {
            function(connection)
        } catch (e: Exception) {
            throw e
        } finally {
            connection.close()
        }

    }
}

class FixedConnectionProvider(val connection: DBConnection) : ConnectionProvider {
    override suspend fun <T> withConnection(function: suspend (DBConnection) -> T): T = function(connection)
}

interface TransactionProvider : ConnectionProvider {
    suspend fun <T> transaction(function: suspend (ConnectionProvider) -> T): T
}

interface ConnectionProvider {

    suspend fun <T> withConnection(function: suspend (DBConnection) -> T): T
}

interface DBConnection {
    fun createStatement(sql: String): Statement
    fun createInsertStatement(sql: String): Statement
    suspend fun beginTransaction(): DBTransaction
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

interface DBTransaction {
    suspend fun rollbackTransaction()
    suspend fun commitTransaction()
}
