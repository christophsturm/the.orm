package r2dbcfun.exp


import io.kotest.core.spec.style.FunSpec
import io.vertx.pgclient.PgConnectOptions
import io.vertx.reactivex.pgclient.PgPool
import io.vertx.reactivex.sqlclient.Row
import io.vertx.reactivex.sqlclient.RowSet
import io.vertx.sqlclient.PoolOptions
import kotlinx.coroutines.rx2.await
import r2dbcfun.test.preparePostgresDB

/*
 * just playing with the vertx pg client api here. maybe at some point r2dbcfun will support this too.
 */
class VertxTest : FunSpec({
    test("vertx") {
        val (databaseName, host, port) = preparePostgresDB()
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
        println("Got " + result.size().toString() + " rows ")
        client.close()
    }
})
