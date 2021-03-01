package r2dbcfun.dbio.vertx

import failfast.FailFast
import failfast.describe
import io.mockk.mockk
import io.vertx.pgclient.PgConnectOptions
import io.vertx.reactivex.pgclient.PgPool
import io.vertx.reactivex.sqlclient.PreparedQuery
import io.vertx.reactivex.sqlclient.Row
import io.vertx.reactivex.sqlclient.RowSet
import io.vertx.reactivex.sqlclient.SqlClient
import io.vertx.sqlclient.PoolOptions
import r2dbcfun.dbio.ConnectionProvider
import r2dbcfun.dbio.DBConnection
import r2dbcfun.dbio.DBResult
import r2dbcfun.dbio.Statement
import r2dbcfun.test.DBS
import strikt.api.expectThat
import strikt.assertions.isEqualTo

fun main() {
    FailFast.runTest()
}
object VertxConnectionTest {
    val context = describe(VertxConnection::class) {
        it("works with ConnectionProvider") {
            ConnectionProvider(VertxConnection(mockk()))
        }
        describe("with a database connection") {
            val (databaseName, host, port) = DBS.psql13.preparePostgresDB()
            val connectOptions = PgConnectOptions()
                .setPort(port)
                .setHost(host)
                .setDatabase(databaseName)
                .setUser("test")
                .setPassword("test")

            val client: SqlClient = autoClose(PgPool.pool(connectOptions, PoolOptions().setMaxSize(5))) { it.close() }
            val connection = VertxConnection(client)

            itWill("run prepared queries") {
                val result: DBResult =
                    connection.createStatement("SELECT * FROM users WHERE id=$1").bind(0, 1).execute()
//                expectThat(result.size()).isEqualTo(0)
            }
            itWill("can insert with autoincrement") {
                val result =
                    connection.createStatement("insert into users(name) values ($1)").bind(0, "belle").executeInsert()
                expectThat(result).isEqualTo(1)
/*
                expectThat(result.size()).isEqualTo(1)
                expectThat(result.columnsNames()).containsExactly("id")
                expectThat(result.single().get(Integer::class.java, "id").toInt()).isEqualTo(1)

 */
            }
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

    override suspend fun executeInsert(): Long {
        TODO("Not yet implemented")
    }

    override suspend fun executeInsert(types: List<Class<*>>, values: Sequence<Any>): Long {
        TODO("Not yet implemented")
    }

}

