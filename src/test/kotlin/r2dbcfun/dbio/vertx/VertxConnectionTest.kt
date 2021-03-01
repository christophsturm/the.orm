package r2dbcfun.dbio.vertx

import failfast.FailFast
import failfast.describe
import io.mockk.mockk
import io.vertx.reactivex.sqlclient.PreparedQuery
import io.vertx.reactivex.sqlclient.Row
import io.vertx.reactivex.sqlclient.RowSet
import io.vertx.reactivex.sqlclient.SqlClient
import io.vertx.reactivex.sqlclient.Tuple
import kotlinx.coroutines.rx2.await
import r2dbcfun.dbio.ConnectionProvider
import r2dbcfun.dbio.DBConnection
import r2dbcfun.dbio.DBResult
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
    override suspend fun executeSelect(parameterValues: Sequence<Any>, sql: String): DBResult {
        TODO("Not yet implemented")
    }

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
    override fun bind(idx: Int, property: Any): Statement {
        TODO("Not yet implemented")
    }

    override fun bind(field: String, property: Any): Statement {
        TODO("Not yet implemented")
    }

    override suspend fun execute(): DBResult {
        TODO("Not yet implemented")
    }

    override fun bindNull(index: Int, dbClass: Class<out Any>): Statement {
        TODO("Not yet implemented")
    }


    override suspend fun executeInsert(types: List<Class<*>>, values: Sequence<Any?>): Long {
        return preparedQuery.rxExecute(Tuple.from(values.toList())).await().single()
            .get(java.lang.Long::class.java, "id").toLong()
    }

}

