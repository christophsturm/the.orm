package r2dbcfun.test

import failfast.ContextDSL
import failfast.RootContext
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.vertx.pgclient.PgConnectOptions
import io.vertx.reactivex.pgclient.PgPool
import io.vertx.reactivex.sqlclient.SqlClient
import io.vertx.sqlclient.PoolOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.withContext
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer
import r2dbcfun.dbio.TransactionProvider
import r2dbcfun.dbio.TransactionalConnectionProvider
import r2dbcfun.dbio.r2dbc.R2DbcDBConnectionFactory
import r2dbcfun.dbio.vertx.VertxDBConnectionFactory
import r2dbcfun.test.TestConfig.TEST_POOL_SIZE
import java.sql.DriverManager
import java.time.Duration
import java.util.*
import kotlin.reflect.KClass

object TestConfig {
    val ALL_PSQL = System.getenv("ALL_PSQL") != null
    val H2_ONLY = System.getenv("H2_ONLY") != null
    val TEST_POOL_SIZE = 2
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

    class R2DBCPostgresFactory(private val psqlContainer: PSQLContainer) : TestDatabase {
        override val name = "R2DBC-${psqlContainer.dockerImage}"

        override fun createDB(): ConnectionProviderFactory {
            val db = psqlContainer.preparePostgresDB()
            return R2dbcConnectionProviderFactory(
                ConnectionFactories.get("r2dbc:postgresql://test:test@${db.host}:${db.port}/${db.databaseName}"),
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
            dropDb()
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
    } else listOf(h2) + postgreSQLContainers.map { R2DBCPostgresFactory(it) } + postgreSQLContainers.map {
        VertxPSQLTestDatabase(
            it
        )
    }
    val unstableDatabases: List<TestDatabase> = listOf()

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
    override suspend fun create(): TransactionProvider {
        val client = PgPool.pool(poolOptions, PoolOptions().setMaxSize(TEST_POOL_SIZE))
        clients.add(client)
        return TransactionalConnectionProvider(VertxDBConnectionFactory(client))
    }


    override suspend fun close() {
        clients.forEach {
            it.close()
        }
        db.close()
    }

}


class R2dbcConnectionProviderFactory(
    private val connectionFactory: ConnectionFactory,
    private val closable: AutoCloseable? = null
) : ConnectionProviderFactory {
    private val pools = mutableListOf<ConnectionPool>()
    override suspend fun create(): TransactionProvider {
        val pool = ConnectionPool(
            ConnectionPoolConfiguration.builder(connectionFactory)
                .maxIdleTime(Duration.ofMillis(1000))
                .maxSize(TEST_POOL_SIZE)
                .build()
        )
        pools.add(pool)
        return TransactionalConnectionProvider(R2DbcDBConnectionFactory(pool))
    }

    override suspend fun close() {
        val poolMetrics = buildString {
            pools.forEach {
                val metrics = it.metrics.get()
                append("allocatedSize: ${metrics.allocatedSize()}")
                append(" acquiredSize: ${metrics.acquiredSize()}")
                it.disposeLater().awaitFirstOrNull()
                append("\nallocatedSize: ${metrics.allocatedSize()}")
                append(" acquiredSize: ${metrics.acquiredSize()}")
                append(" disposed: ${it.isDisposed}")
            }
        }
        try {
            closable?.close()
        } catch (e: Exception) {
            println("ERROR dropping database. pool metrics:${poolMetrics}")
        }
    }

}

interface ConnectionProviderFactory {
    suspend fun create(): TransactionProvider
    suspend fun close()

}

suspend fun ContextDSL.forAllDatabases(
    databases: List<DBTestUtil.TestDatabase>,
    tests: suspend ContextDSL.(suspend () -> TransactionProvider) -> Unit
) {
    databases.map { db ->
        context("on ${db.name}") {
            val createDB = autoClose(db.createDB()) { it.close() }
            val connectionFactory: suspend () -> TransactionProvider =
                { createDB.create() }
            tests(connectionFactory)
        }
    }
}


fun describeOnAllDbs(
    subject: KClass<*>,
    databases: List<DBTestUtil.TestDatabase>,
    disabled: Boolean = false,
    tests: suspend ContextDSL.(suspend () -> TransactionProvider) -> Unit
) = describeOnAllDbs("the ${subject.simpleName!!}", databases, disabled, tests)

fun describeOnAllDbs(
    contextName: String,
    databases: List<DBTestUtil.TestDatabase>,
    disabled: Boolean = false,
    tests: suspend ContextDSL.(suspend () -> TransactionProvider) -> Unit
): List<RootContext> {
    return databases.map { testDB ->
        RootContext("$contextName on ${testDB.name}", disabled) {
            val createDB = autoClose(withContext(Dispatchers.IO) { testDB.createDB() }) { it.close() }
            val connectionFactory: suspend () -> TransactionProvider =
                { createDB.create() }
            tests(connectionFactory)
        }
    }
}

