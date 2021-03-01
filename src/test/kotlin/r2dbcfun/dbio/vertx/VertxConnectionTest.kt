package r2dbcfun.dbio.vertx

import failfast.FailFast
import failfast.describe
import io.mockk.mockk
import io.vertx.reactivex.sqlclient.PreparedQuery
import io.vertx.reactivex.sqlclient.Row
import io.vertx.reactivex.sqlclient.RowSet
import io.vertx.reactivex.sqlclient.SqlClient
import io.vertx.reactivex.sqlclient.Tuple
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx2.await
import r2dbcfun.dbio.ConnectionProvider
import r2dbcfun.dbio.DBConnection
import r2dbcfun.dbio.DBResult
import r2dbcfun.dbio.DBRow
import r2dbcfun.dbio.LazyResult
import r2dbcfun.dbio.Statement

fun main() {
    FailFast.runTest()
}
object VertxConnectionTest {
    val context = describe(VertxConnection::class) {
        it("works with ConnectionProvider") {
            ConnectionProvider(VertxConnection(mockk()))
        }
    }
}

class VertxConnection(val client: SqlClient) : DBConnection {
    override suspend fun beginTransaction() {
        TODO("Not yet implemented")
    }

    override suspend fun commitTransaction() {
        TODO("Not yet implemented")
    }

    override fun createStatement(sql: String): Statement {
        return VertxStatement(client.preparedQuery(sql))
    }

    override suspend fun rollbackTransaction() {
        TODO("Not yet implemented")
    }

    override fun createInsertStatement(sql: String): Statement {
        return createStatement("$sql returning id")
    }

}

class VertxStatement(val preparedQuery: PreparedQuery<RowSet<Row>>) :
    Statement {
    override suspend fun execute(types: List<Class<*>>, values: Sequence<Any?>): DBResult {
        val rowSet = preparedQuery.rxExecute(Tuple.from(values.toList())).await()
        return VertxResult(rowSet)
    }
}

class VertxResult(private val rows: RowSet<Row>) : DBResult {
    override suspend fun rowsUpdated(): Int {
        return rows.rowCount()
    }

    override suspend fun <T : Any> map(mappingFunction: (t: DBRow) -> T): Flow<T> {
        val flow: Flow<Row> = rows.asFlow()
        return flow.map { mappingFunction(VertxRow(it)) }
    }

}

class VertxRow(val row: Row) : DBRow {
    override fun getLazy(key: String): LazyResult<Any?> {
        val result = get(key, Object::class.java)
        return LazyResult { result }
    }

    override fun <T> get(key: String, type: Class<T>): T? {
        return row.get(type, key)
    }

}

