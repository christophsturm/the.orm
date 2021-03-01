package r2dbcfun.dbio

import kotlinx.coroutines.flow.Flow
import r2dbcfun.transaction.transaction

class ConnectionProvider(val dbConnection: DBConnection) {
    suspend fun <T> transaction(function: suspend () -> T): T = transaction(dbConnection, function)
}

interface DBConnection {
    suspend fun executeSelect(
        parameterValues: Sequence<Any>,
        sql: String
    ): DBResult

    suspend fun beginTransaction()
    suspend fun commitTransaction()
    fun createStatement(sql: String): Statement
    suspend fun rollbackTransaction()
}

interface Statement {
    fun bind(idx: Int, property: Any): Statement
    fun bind(field: String, property: Any): Statement
    suspend fun execute(): DBResult
    fun bindNull(index: Int, dbClass: Class<out Any>): Statement
    suspend fun executeInsert(): Long
    suspend fun executeInsert(types: List<Class<*>>, values: Sequence<Any?>): Long
}

interface DBResult {
    suspend fun rowsUpdated(): Int
    fun <T : Any> map(mappingFunction: (t: DBRow) -> T): Flow<T>
}

interface DBRow {
    fun getLazy(key: String): LazyResult<Any?>
    fun <T> get(key: String, type: Class<T>): T?
}

class LazyResult<T>(val get: suspend () -> T) {
    suspend fun resolve() = get()
}
