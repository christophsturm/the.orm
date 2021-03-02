package r2dbcfun.test

import failfast.ContextDSL
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.vertx.pgclient.PgConnectOptions
import io.vertx.reactivex.pgclient.PgPool
import io.vertx.reactivex.sqlclient.SqlClient
import io.vertx.sqlclient.PoolOptions
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer
import r2dbcfun.dbio.ConnectionProvider
import r2dbcfun.dbio.TransactionalConnectionProvider
import r2dbcfun.dbio.r2dbc.R2dbcConnectionFactory
import r2dbcfun.dbio.vertx.VertxConnectionFactory
import java.sql.DriverManager
import java.time.Duration
import java.util.*

object TestConfig {
    val ALL_PSQL = System.getenv("ALL_PSQL") != null
    val H2_ONLY = System.getenv("H2_ONLY") != null
}

open class DBTestUtil(val databaseName: String) {
    interface TestDatabase {
        val name: String

        fun createDB(): ConnectionProviderFactory
        fun prepare() {}
    }

    inner class H2TestDatabase : TestDatabase {
        override val name = "H2"

        override fun createDB(): ConnectionProviderFactory {
            val uuid = UUID.randomUUID()
            val databaseName = "$databaseName$uuid"
            val jdbcUrl = "jdbc:h2:mem:$databaseName;DB_CLOSE_DELAY=-1"
            val flyway = Flyway.configure().dataSource(jdbcUrl, "", "").load()
            flyway.migrate()
            return R2dbcConnectionProviderFactory(ConnectionFactories.get("r2dbc:h2:mem:///$databaseName;DB_CLOSE_DELAY=-1"))
        }
    }


    inner class PSQLContainer(val dockerImage: String) {
        fun prepare() {
            dockerContainer
        }

        private val dockerContainer: PostgreSQLContainer<Nothing> by
        lazy {
            PostgreSQLContainer<Nothing>(dockerImage).apply {
// WIP           setCommand("postgres", "-c", "fsync=off", "-c", "max_connections=200")
                withReuse(true)
                start()
            }
        }


        fun preparePostgresDB(): PostgresDb {
            Class.forName("org.postgresql.Driver")
            val uuid = UUID.randomUUID()
            val databaseName = "$databaseName$uuid".replace("-", "_")
            // testcontainers says that it returns an ip address but it returns a host name.
            val host = dockerContainer.containerIpAddress.let { if (it == "localhost") "127.0.0.1" else it }
            val port = dockerContainer.getMappedPort(5432)
            val postgresDb = PostgresDb(databaseName, host, port)
            postgresDb.createDb()

            val flyway =
                Flyway.configure()
                    .dataSource("jdbc:postgresql://$host:$port/$databaseName", "test", "test")
                    .load()
            flyway.migrate()
            return postgresDb
        }

    }

    class R2DBCPostgresFactory(val psqlContainer: PSQLContainer) : TestDatabase {
        override val name = "R2DBC-${psqlContainer.dockerImage}"

        override fun createDB(): ConnectionProviderFactory {
            val db = psqlContainer.preparePostgresDB()
            return R2dbcConnectionProviderFactory(
                ConnectionFactories.get("r2dbc:postgresql://test:test@${db.host}:${db.port}/${db.databaseName}?initialSize=1&maxLifeTime=PT0S"),
                db
            )
        }

    }

    data class PostgresDb(val databaseName: String, val host: String, val port: Int) : AutoCloseable {
        fun createDb() {
            executeSql("create database $databaseName")
        }

        private fun dropDb() {
            executeSql("drop database $databaseName")
        }

        private fun executeSql(command: String) {
            val db =
                DriverManager.getConnection(
                    "jdbc:postgresql://$host:$port/postgres",
                    "test",
                    "test"
                )
            @Suppress("SqlNoDataSourceInspection")
            db.createStatement().executeUpdate(command)
            db.close()
        }

        override fun close() {
            try {
                dropDb()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }

    private val h2 = H2TestDatabase()
    val psql13 = PSQLContainer("postgres:13-alpine")
    val postgreSQLContainers = if (TestConfig.ALL_PSQL) listOf(
        psql13,
        PSQLContainer("postgres:12-alpine"),
        PSQLContainer("postgres:11-alpine"),
        PSQLContainer("postgres:10-alpine"),
        PSQLContainer("postgres:9-alpine")
    )
    else
        listOf(psql13)

    val databases = if (TestConfig.H2_ONLY) {
        listOf(h2)
    } else listOf(h2) + postgreSQLContainers.map { R2DBCPostgresFactory(it) }
    val unstableDatabases = listOf<TestDatabase>()//postgreSQLContainers.map { VertxPSQLTestDatabase(it) }

    inner class VertxPSQLTestDatabase(val psql: PSQLContainer) : TestDatabase {
        override val name = "Vertx-${psql.dockerImage}"
        override fun createDB(): ConnectionProviderFactory {
            val database = psql.preparePostgresDB()
            val connectOptions = PgConnectOptions()
                .setPort(database.port)
                .setHost(database.host)
                .setDatabase(database.databaseName)
                .setUser("test")
                .setPassword("test")

            return VertxConnectionProviderFactory(connectOptions, database)
        }
    }

}

class VertxConnectionProviderFactory(val poolOptions: PgConnectOptions, val db: AutoCloseable) :
    ConnectionProviderFactory {
    val clients = mutableListOf<SqlClient>()
    override suspend fun create(): ConnectionProvider {
        val client = PgPool.pool(poolOptions, PoolOptions().setMaxSize(5))
        clients.add(client)
        return TransactionalConnectionProvider(VertxConnectionFactory(client))
    }


    override suspend fun close() {
        clients.forEach {
            it.close()
        }
        db.close()
    }

}


class R2dbcConnectionProviderFactory(
    val connectionFactory: ConnectionFactory,
    private val closable: AutoCloseable? = null
) : ConnectionProviderFactory {
    private val connections = mutableListOf<ConnectionPool>()
    override suspend fun create(): ConnectionProvider {
        val pool = ConnectionPool(
            ConnectionPoolConfiguration.builder(connectionFactory)
                .maxIdleTime(Duration.ofMillis(1000))
                .maxSize(5)
                .build()
        )
        connections.add(pool)
        return TransactionalConnectionProvider(R2dbcConnectionFactory(pool))
    }

    override suspend fun close() {
        connections.forEach {
            it.close()
        }
        closable?.close()
    }

}

interface ConnectionProviderFactory {
    suspend fun create(): ConnectionProvider
    suspend fun close()

}

suspend fun ContextDSL.forAllDatabases(
    dbs: DBTestUtil,
    additionalDatabases: List<DBTestUtil.TestDatabase> = listOf(),
    tests: suspend ContextDSL.(suspend () -> ConnectionProvider) -> Unit
) {
    (dbs.databases + additionalDatabases).map { db ->
        context("on ${db.name}") {
            val createDB = autoClose(db.createDB()) { it.close() }
            val connectionFactory: suspend () -> ConnectionProvider =
                { createDB.create() }
            tests(connectionFactory)
        }
    }
}

