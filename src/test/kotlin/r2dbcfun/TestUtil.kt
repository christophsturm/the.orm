package r2dbcfun

import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import org.flywaydb.core.Flyway
import org.h2.jdbcx.JdbcDataSource
import java.util.*

fun prepareDB(): ConnectionFactory {
    val dataSource = JdbcDataSource()
    val uuid = UUID.randomUUID()
    dataSource.setURL("jdbc:h2:mem:r2dbc-test$uuid;DB_CLOSE_DELAY=-1")
    val flyway = Flyway.configure().dataSource(dataSource).load()
    flyway.migrate()
    return ConnectionFactories.get("r2dbc:h2:mem:///r2dbc-test$uuid;DB_CLOSE_DELAY=-1")
}
