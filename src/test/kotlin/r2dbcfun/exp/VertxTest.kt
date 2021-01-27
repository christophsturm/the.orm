package r2dbcfun.exp


import failfast.describe
import io.vertx.pgclient.PgConnectOptions
import io.vertx.reactivex.pgclient.PgPool
import io.vertx.reactivex.sqlclient.Row
import io.vertx.reactivex.sqlclient.RowSet
import io.vertx.sqlclient.PoolOptions
import kotlinx.coroutines.rx2.await
import r2dbcfun.TestConfig
import r2dbcfun.test.DBS
import strikt.api.expectThat
import strikt.assertions.isEqualTo

/*
 * just playing with the vertx pg client api here. maybe at some point r2dbcfun will support this too.
 */
object VertxTest {
    val context = describe("vertx support", disabled = TestConfig.H2_ONLY) {

        it("is not yet supported but seems to work with coroutines") {
            val (databaseName, host, port) = DBS.psql13.preparePostgresDB()
            val connectOptions = PgConnectOptions()
                .setPort(port)
                .setHost(host)
                .setDatabase(databaseName)
                .setUser("test")
                .setPassword("test")

// Pool options
            val poolOptions = PoolOptions().setMaxSize(5)

// Create the client pool
            val client = PgPool.pool(connectOptions, poolOptions)

// A simple query
            @Suppress("SqlResolve")
            val result: RowSet<Row> = client
                .query("SELECT * FROM users WHERE id=1").rxExecute().await()
            expectThat(result.size()).isEqualTo(0)
            client.close()
        }
    }
}
