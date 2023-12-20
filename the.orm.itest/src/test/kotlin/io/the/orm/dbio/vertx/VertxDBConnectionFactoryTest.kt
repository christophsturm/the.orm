package io.the.orm.dbio.vertx

import failgood.Ignored
import failgood.Test
import failgood.describe
import io.the.orm.test.DBS
import io.the.orm.test.TestUtilConfig
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.pgclient.PgBuilder
import io.vertx.pgclient.PgConnectOptions
import io.vertx.sqlclient.PoolOptions

@Test
class VertxDBConnectionFactoryTest {
    val context =
        describe<VertxDBConnectionFactory>(
            ignored =
                if (TestUtilConfig.H2_ONLY) Ignored.Because("running in h2 only mode") else null
        ) {
            it("can create connections from a pool") {
                val (databaseName, port, host) = DBS.psql16.preparePostgresDB()
                val connectOptions =
                    PgConnectOptions()
                        .setPort(port)
                        .setHost(host)
                        .setDatabase(databaseName)
                        .setUser("test")
                        .setPassword("test")

                val pool =
                    autoClose(
                        PgBuilder.pool()
                            .with(PoolOptions().setMaxSize(5))
                            .connectingTo(connectOptions)
                            .build()!!
                    ) {
                        it.close().coAwait()
                    }

                VertxDBConnectionFactory(pool).getConnection()
            }
        }
}
