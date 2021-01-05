package r2dbcfun.test

import failfast.ContextDSL
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer
import r2dbcfun.TestConfig
import r2dbcfun.TestConfig.ALLPSQL
import java.sql.DriverManager
import java.util.*

interface TestDatabase {
    val name: String

    fun prepare(): ConnectionFactory
}

class H2TestDatabase : TestDatabase {
    override val name = "H2"

    override fun prepare(): ConnectionFactory {
        val uuid = UUID.randomUUID()
        val databaseName = "r2dbc-test$uuid"
        val jdbcUrl = "jdbc:h2:mem:$databaseName;DB_CLOSE_DELAY=-1"
        val flyway = Flyway.configure().dataSource(jdbcUrl, "", "").load()
        flyway.migrate()
        return ConnectionFactories.get("r2dbc:h2:mem:///$databaseName;DB_CLOSE_DELAY=-1")
    }
}


class PSQLTestDatabase(val dockerImage: String) : TestDatabase {
    override val name = dockerImage

    val postgresqlcontainer: PostgreSQLContainer<Nothing> by
    lazy {
        PostgreSQLContainer<Nothing>(dockerImage).apply {
// WIP           setCommand("postgres", "-c", "fsync=off", "-c", "max_connections=200")
            withReuse(true)
            start()
        }
    }

    override fun prepare(): ConnectionFactory {
        val (databaseName, host, port) = preparePostgresDB()
        return ConnectionFactories.get("r2dbc:pool:postgresql://test:test@$host:$port/$databaseName?initialSize=1")
    }

    fun preparePostgresDB(): NameHostAndPort {
        Class.forName("org.postgresql.Driver")
        val uuid = UUID.randomUUID()
        val databaseName = "r2dbctest$uuid".replace("-", "_")
        // testcontainers says that it returns an ip address but it returns a host name.
        val host = postgresqlcontainer.containerIpAddress.let { if (it == "localhost") "127.0.0.1" else it }
        val port = postgresqlcontainer.getMappedPort(5432)
        val db =
            DriverManager.getConnection("jdbc:postgresql://$host:$port/postgres", "test", "test")
        db.createStatement().executeUpdate("create database $databaseName")
        db.close()

        val flyway =
            Flyway.configure()
                .dataSource("jdbc:postgresql://$host:$port/$databaseName", "test", "test")
                .load()
        flyway.migrate()
        return NameHostAndPort(databaseName, host, port)
    }
}

data class NameHostAndPort(val databaseName: String, val host: String, val port: Int)

val h2 = H2TestDatabase()
val psql13 = PSQLTestDatabase("postgres:13-alpine")
val databases = when {
    TestConfig.H2_ONLY -> {
        listOf(h2)
    }
    ALLPSQL -> {
        listOf(
            h2, psql13,
            PSQLTestDatabase("postgres:12-alpine"),
            PSQLTestDatabase("postgres:11-alpine"),
            PSQLTestDatabase("postgres:10-alpine"),
            PSQLTestDatabase("postgres:9-alpine")
        )
    }
    else -> listOf(h2, psql13)
}

suspend fun ContextDSL.forAllDatabases(tests: suspend ContextDSL.(ConnectionFactory) -> Unit) {
    databases.map { db ->
        context("on ${db.name}") {
            val connectionFactory = db.prepare()
            tests(connectionFactory)
        }
    }

}
