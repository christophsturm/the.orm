package r2dbcfun.dbio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.single
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
    fun createInsertStatement(sql: String): Statement
}

interface Statement {
    fun bind(idx: Int, property: Any): Statement
    fun bind(field: String, property: Any): Statement
    suspend fun execute(): DBResult
    fun bindNull(index: Int, dbClass: Class<out Any>): Statement
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
