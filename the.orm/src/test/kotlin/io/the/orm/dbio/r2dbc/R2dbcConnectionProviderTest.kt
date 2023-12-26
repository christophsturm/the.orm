package io.the.orm.dbio.r2dbc

import failgood.Test
import failgood.tests
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import java.time.Duration

@Test
class R2dbcConnectionProviderTest {
    val context = tests {
        test("creates connections from a connection Pool") {
            val connectionFactory = ConnectionFactories.get("r2dbc:h2:mem:///any")
            val pool =
                ConnectionPool(
                    ConnectionPoolConfiguration.builder(connectionFactory)
                        .maxIdleTime(Duration.ofMillis(1000))
                        .maxSize(20)
                        .build()
                )

            R2DbcDBConnectionFactory(pool).getConnection()
        }
    }
}
