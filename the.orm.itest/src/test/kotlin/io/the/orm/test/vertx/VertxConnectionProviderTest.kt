package io.the.orm.test.vertx

import failgood.Test
import failgood.describe
import io.the.orm.dbio.DBConnection
import io.the.orm.dbio.vertx.VertxDBConnectionFactory
import io.the.orm.test.DBS
import io.the.orm.test.TestUtilConfig
import io.vertx.kotlin.coroutines.await
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions

@Test
class VertxDBConnectionProviderTest {
    val context = describe(VertxDBConnectionFactory::class, disabled = TestUtilConfig.H2_ONLY) {
        it("can create connections from a pool") {
            val (databaseName, host, port) = DBS.psql14.preparePostgresDB()
            val connectOptions = PgConnectOptions()
                .setPort(port)
                .setHost(host)
                .setDatabase(databaseName)
                .setUser("test")
                .setPassword("test")

            val pool = autoClose(PgPool.pool(connectOptions, PoolOptions().setMaxSize(5))) { it.close().await() }

            @Suppress("UNUSED_VARIABLE")
            val connection: DBConnection = VertxDBConnectionFactory(pool).getConnection()
        }
    }
}
