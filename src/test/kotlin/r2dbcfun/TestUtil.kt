package r2dbcfun

import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import java.sql.Connection
import java.sql.DriverManager
import java.util.*
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer

fun prepareH2(): ConnectionFactory {
    val uuid = UUID.randomUUID()
    val databaseName = "r2dbc-test$uuid"
    val jdbcUrl = "jdbc:h2:mem:$databaseName;DB_CLOSE_DELAY=-1"
    val flyway = Flyway.configure().dataSource(jdbcUrl, "", "").load()
    flyway.migrate()
    return ConnectionFactories.get("r2dbc:h2:mem:///$databaseName;DB_CLOSE_DELAY=-1")
}

val container: PostgreSQLContainer<Nothing> by
    lazy {
        PostgreSQLContainer<Nothing>("postgres:13.0").apply {
            withReuse(true)
            start()
        }
    }

fun preparePostgreSQL(): ConnectionFactory {
    Class.forName("org.postgresql.Driver")
    val uuid = UUID.randomUUID()
    val databaseName = "r2dbctest$uuid".replace("-", "_")
    val host = container.containerIpAddress
    val port = container.getMappedPort(5432)
    val db: Connection =
        DriverManager.getConnection("jdbc:postgresql://$host:$port/postgres", "test", "test")
    db.createStatement().executeUpdate("create database $databaseName")

    val flyway =
        Flyway.configure()
            .dataSource("jdbc:postgresql://$host:$port/$databaseName", "test", "test")
            .load()
    flyway.migrate()
    return ConnectionFactories.get("r2dbc:postgresql://test:test@$host:$port/$databaseName")
}
