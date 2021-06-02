package io.the.orm.dbio.r2dbc

import failgood.describe
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import io.the.orm.dbio.DBConnection
import java.time.Duration


object R2dbcConnectionProviderTest {
    val context = describe(R2DbcDBConnectionFactory::class) {
        test("creates connections from a connection Pool") {
            val connectionFactory = ConnectionFactories.get("r2dbc:h2:mem:///any")
            val pool = ConnectionPool(
                ConnectionPoolConfiguration.builder(connectionFactory)
                    .maxIdleTime(Duration.ofMillis(1000))
                    .maxSize(20)
                    .build()
            )

            @Suppress("UNUSED_VARIABLE")
            val connection: DBConnection = R2DbcDBConnectionFactory(pool).getConnection()
        }
    }
}
