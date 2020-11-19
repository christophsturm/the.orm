package r2dbcfun.test

import io.kotest.core.TestConfiguration
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.FunSpecContextScope
import io.kotest.inspectors.forAll
import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitSingle
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.DriverManager
import java.util.*

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
        PostgreSQLContainer<Nothing>("postgres:13").apply {
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
    val db =
        DriverManager.getConnection("jdbc:postgresql://$host:$port/postgres", "test", "test")
    db.createStatement().executeUpdate("create database $databaseName")

    val flyway =
        Flyway.configure()
            .dataSource("jdbc:postgresql://$host:$port/$databaseName", "test", "test")
            .load()
    flyway.migrate()
    return ConnectionFactories.get("r2dbc:postgresql://test:test@$host:$port/$databaseName")
}

data class Database(val name: String, val makeConnectionFactory: () -> ConnectionFactory)
val databases = listOf(Database("h2") { prepareH2() }, Database("psql") { preparePostgreSQL() })

fun forAllDatabases(
    funSpec: FunSpec,
    testName: String,
    tests: suspend FunSpecContextScope.(Connection) -> Unit
) {
    databases.forAll { db ->
        funSpec.context("$testName on ${db.name}") {
            val connection = funSpec.autoClose(db.makeConnectionFactory().create().awaitSingle()) { it.close() }
            tests(connection)
        }
    }
}

fun <T : Any> TestConfiguration.autoClose(wrapped: T, function: (T) -> Unit): T {
    autoClose(object : AutoCloseable {
        override fun close() {
            function(wrapped)
        }
    })
    return wrapped
}
