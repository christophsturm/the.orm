package io.the.orm

import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import io.the.orm.dbio.DBConnectionFactory
import io.the.orm.dbio.r2dbc.R2DbcDBConnectionFactory
import io.the.orm.dbio.vertx.VertxDBConnectionFactory
import io.vertx.pgclient.PgBuilder
import io.vertx.pgclient.PgConnectOptions
import io.vertx.sqlclient.PoolOptions
import java.time.Duration

/** utility method for creating connection factories for vertx and r2dbc */
@Suppress("unused")
object TheOrm {
    fun openR2dbcUrl(r2dbcUrl: String): DBConnectionFactory {
        val connectionFactory = ConnectionFactories.get(r2dbcUrl)
        val pool =
            ConnectionPool(
                ConnectionPoolConfiguration.builder(connectionFactory)
                    .maxIdleTime(Duration.ofMillis(1000))
                    .maxSize(20)
                    .build()
            )

        return R2DbcDBConnectionFactory(pool)
    }

    fun openVertxPSQL(db: String, user: String, host: String = "127.0.0.1"): DBConnectionFactory {
        val connectOptions = PgConnectOptions().setHost(host).setDatabase(db).setUser(user)

        val pool =
            PgBuilder.pool().with(PoolOptions().setMaxSize(5)).connectingTo(connectOptions).build()

        return VertxDBConnectionFactory(pool)
    }
}
