package r2dbcfun.test

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.FunSpecContextScope
import io.kotest.inspectors.forAll
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitSingle
import org.flywaydb.core.Flyway
import org.reactivestreams.Publisher
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.Connection
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

data class Database(val name: String, val function: () -> ConnectionFactory)
val databases = listOf(Database("h2") { prepareH2() }, Database("psql") { preparePostgreSQL() })

fun forAllDatabases(
    funSpec: FunSpec,
    testName: String,
    tests: suspend FunSpecContextScope.(io.r2dbc.spi.Connection) -> Unit
) {
    databases.forAll {
        funSpec.context("$testName on ${it.name}") {
            val connection =
                this.run {
                    val connectionClosable =
                        funSpec.autoClose(
                            WrapAutoClosable(it.function().create().awaitSingle())
                            { connection: io.r2dbc.spi.Connection -> connection.close() }
                        )
                    connectionClosable.wrapped
                }
            this.tests(connection)
        }
    }
}

class WrapAutoClosable<T : Any>(val wrapped: T, val function: (T) -> Publisher<Void>) :
    AutoCloseable {

    override fun close() {
        function(wrapped)
    }
}
