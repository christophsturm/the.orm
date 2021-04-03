package r2dbcfun.exp.vertx


import failfast.FailFast
import failfast.describe
import io.vertx.pgclient.PgConnectOptions
import io.vertx.reactivex.pgclient.PgPool
import io.vertx.reactivex.sqlclient.Row
import io.vertx.reactivex.sqlclient.RowSet
import io.vertx.reactivex.sqlclient.SqlClient
import io.vertx.reactivex.sqlclient.Tuple
import io.vertx.sqlclient.PoolOptions
import kotlinx.coroutines.rx2.await
import r2dbcfun.TestConfig
import r2dbcfun.test.DBS
import r2dbcfun.test.schemaSql
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo

fun main() {
    FailFast.runTest()
}

@Suppress("SqlNoDataSourceInspection", "SqlResolve")
object VertxTest {
    val context = describe("vertx support", disabled = TestConfig.H2_ONLY) {
        val db by dependency({ DBS.psql13.preparePostgresDB() }) { it.close() }


        val client: SqlClient by dependency({
            PgPool.pool(
                PgConnectOptions()
                    .setPort(db.port)
                    .setHost(db.host)
                    .setDatabase(db.databaseName)
                    .setUser("test")
                    .setPassword("test"), PoolOptions().setMaxSize(5)
            ).also { it.query(schemaSql).rxExecute().await() }
        }) { it.close() }
        it("can run sql queries") {
            val result: RowSet<Row> = client.query("SELECT * FROM users WHERE id=1").rxExecute().await()
            expectThat(result.size()).isEqualTo(0)
        }
        it("can run prepared queries") {
            val result: RowSet<Row> =
                client.preparedQuery("SELECT * FROM users WHERE id=$1").rxExecute(Tuple.of(1)).await()
            expectThat(result.size()).isEqualTo(0)
        }
        it("can insert with autoincrement") {
            val result: RowSet<Row> =
                client.preparedQuery("insert into users(name) values ($1) returning id").rxExecute(Tuple.of("belle"))
                    .await()
            expectThat(result.size()).isEqualTo(1)
            expectThat(result.columnsNames()).containsExactly("id")
            expectThat(result.single().get(Integer::class.java, "id").toInt()).isEqualTo(1)
        }
    }
}
