package r2dbcfun.dbio.vertx

import failfast.FailFast
import io.vertx.reactivex.sqlclient.PreparedQuery
import io.vertx.reactivex.sqlclient.Row
import io.vertx.reactivex.sqlclient.RowSet
import io.vertx.reactivex.sqlclient.SqlConnection
import io.vertx.reactivex.sqlclient.Transaction
import io.vertx.reactivex.sqlclient.Tuple
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx2.await
import r2dbcfun.dbio.DBConnection
import r2dbcfun.dbio.DBResult
import r2dbcfun.dbio.DBRow
import r2dbcfun.dbio.DBTransaction
import r2dbcfun.dbio.LazyResult
import r2dbcfun.dbio.Statement

fun main() {
    FailFast.runTest()
}

class VertxConnection(private val client: SqlConnection) : DBConnection {
    override fun createStatement(sql: String): Statement {
        return VertxStatement(client.preparedQuery(sql))
    }

    override fun createInsertStatement(sql: String): Statement {
        return createStatement("$sql returning id")
    }


    override suspend fun beginTransaction(): DBTransaction {
        return VertxTransaction(client.rxBegin().await())
    }

    override suspend fun close() {
        client.rxClose().await()
    }

}


class VertxTransaction(val transaction: Transaction) : DBTransaction {
    override suspend fun rollbackTransaction() {
        transaction.rxRollback().await()
    }

    override suspend fun commitTransaction() {
        transaction.rxCommit().await()
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

