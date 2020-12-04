package r2dbcfun.exp

import io.kotest.core.spec.style.FunSpec
import io.vertx.core.AsyncResult
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import kotlinx.coroutines.CompletableDeferred
import r2dbcfun.test.preparePostgresDB


class VertxTest : FunSpec({
    test("vertx") {
        val def = CompletableDeferred<Unit>()   // make sure the test does not exit before the callbacks fired
        val (databaseName, host, port) = preparePostgresDB()
        val connectOptions = PgConnectOptions()
            .setPort(port)
            .setHost(host)
            .setDatabase(databaseName)
            .setUser("test")
            .setPassword("test")

// Pool options
        val poolOptions = PoolOptions()
            .setMaxSize(5)

// Create the client pool
        val client = PgPool.pool(connectOptions, poolOptions)

// A simple query
        @Suppress("SqlResolve")
        client
            .query("SELECT * FROM users WHERE id=1")
            .execute { ar: AsyncResult<RowSet<Row?>> ->
                if (ar.succeeded()) {
                    val result: RowSet<Row?> = ar.result()
                    println("Got " + result.size().toString() + " rows ")
                } else {
                    println("Failure: " + ar.cause().message)
                }

                // Now close the pool
                client.close()
                def.complete(Unit)
            }
        def.await()
    }
})
