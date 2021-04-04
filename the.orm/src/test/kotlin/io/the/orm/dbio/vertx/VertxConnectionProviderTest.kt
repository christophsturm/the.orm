package io.the.orm.dbio.vertx

import failfast.FailFast
import failfast.describe
import io.the.orm.dbio.DBConnection
import io.the.orm.test.DBS
import io.the.orm.test.TestConfig
import io.vertx.pgclient.PgConnectOptions
import io.vertx.reactivex.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import kotlinx.coroutines.rx2.await

fun main() {
    FailFast.runTest()
}

object VertxDBConnectionProviderTest {
    val context = describe(VertxDBConnectionFactory::class, disabled = TestConfig.H2_ONLY) {
        it("can create connections from a pool") {
            val (databaseName, host, port) = DBS.psql13.preparePostgresDB()
            val connectOptions = PgConnectOptions()
                .setPort(port)
                .setHost(host)
                .setDatabase(databaseName)
                .setUser("test")
                .setPassword("test")

            val pool = autoClose(PgPool.pool(connectOptions, PoolOptions().setMaxSize(5))) { it.rxClose().await() }

            @Suppress("UNUSED_VARIABLE")
            val connection: DBConnection = VertxDBConnectionFactory(pool).getConnection()
        }

    }
}

