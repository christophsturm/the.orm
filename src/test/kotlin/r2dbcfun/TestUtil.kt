package r2dbcfun

import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import org.flywaydb.core.Flyway
import org.h2.jdbcx.JdbcDataSource

fun prepareDB(): ConnectionFactory {
    val dataSource = JdbcDataSource()
    dataSource.setURL("jdbc:h2:mem:r2dbc-test;DB_CLOSE_DELAY=-1")
    val flyway = Flyway.configure().dataSource(dataSource).load()
    flyway.clean()
    flyway.migrate()
    return ConnectionFactories.get("r2dbc:h2:mem:///r2dbc-test;DB_CLOSE_DELAY=-1")
}
