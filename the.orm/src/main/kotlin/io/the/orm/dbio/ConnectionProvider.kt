package io.the.orm.dbio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.single

interface ConnectionProvider {
    suspend fun <T> withConnection(function: suspend (DBConnection) -> T): T
}

class FixedConnectionProvider(val connection: DBConnection) : ConnectionProvider {
    override suspend fun <T> withConnection(function: suspend (DBConnection) -> T): T = function(connection)
}

interface TransactionProvider : ConnectionProvider {
    suspend fun <T> transaction(function: suspend (ConnectionProvider) -> T): T
}

interface Statement {
    suspend fun execute(types: List<Class<*>>, values: Sequence<Any?>): DBResult
    suspend fun executeBatch(types: List<Class<*>>, valuesList: Sequence<Sequence<Any?>>): Flow<DBResult>
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
