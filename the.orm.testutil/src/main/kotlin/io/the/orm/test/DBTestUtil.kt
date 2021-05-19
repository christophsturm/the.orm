package io.the.orm.test

import failfast.ContextDSL
import failfast.RootContext
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.the.orm.dbio.TransactionProvider
import io.the.orm.dbio.TransactionalConnectionProvider
import io.the.orm.dbio.r2dbc.R2DbcDBConnectionFactory
import io.the.orm.dbio.vertx.VertxDBConnectionFactory
import io.the.orm.test.TestUtilConfig.TEST_POOL_SIZE
import io.vertx.pgclient.PgConnectOptions
import io.vertx.reactivex.pgclient.PgPool
import io.vertx.reactivex.sqlclient.SqlClient
import io.vertx.sqlclient.PoolOptions
import kotlinx.coroutines.reactive.awaitFirstOrNull
import java.io.BufferedReader
import java.time.Duration
import java.util.*
import kotlin.reflect.KClass

object TestUtilConfig {
    val ALL_PSQL = System.getenv("ALL_PSQL") != null
    val H2_ONLY = System.getenv("H2_ONLY") != null
    const val TEST_POOL_SIZE = 2
}

val schemaSql =
    DBTestUtil::class.java.getResourceAsStream("/db/migration/V1__create_test_tables.sql")!!.bufferedReader()
        .use(BufferedReader::readText)

class DBTestUtil(val databaseName: String) {
    private val h2 = H2TestDatabase()
    val psql13 = PSQLContainer("postgres:13-alpine", databaseName)
    private val postgreSQLContainers = if (TestUtilConfig.ALL_PSQL) listOf(
        psql13,
        PSQLContainer("postgres:12-alpine", databaseName),
        PSQLContainer("postgres:11-alpine", databaseName),
        PSQLContainer("postgres:10-alpine", databaseName),
        PSQLContainer("postgres:9-alpine", databaseName)
    )
    else
        listOf(psql13)

    val databases = if (TestUtilConfig.H2_ONLY) {
        listOf(h2)
    } else listOf(h2) +
            postgreSQLContainers.map { R2DBCPostgresFactory(it) } +
            postgreSQLContainers.map { VertxPSQLTestDatabase(it) }

    @Suppress("unused")
    val unstableDatabases: List<TestDatabase> = listOf()

    interface TestDatabase {
        val name: String

        suspend fun createDB(): ConnectionProviderFactory
        fun prepare() {}
    }

    inner class H2TestDatabase : TestDatabase {
        override val name = "H2"

        override suspend fun createDB(): ConnectionProviderFactory {
            val uuid = UUID.randomUUID()
            val databaseName = "$databaseName$uuid"
            val connectionFactory = ConnectionFactories.get("r2dbc:h2:mem:///$databaseName;DB_CLOSE_DELAY=-1")
            return R2dbcConnectionProviderFactory(connectionFactory)
        }
    }


    class R2DBCPostgresFactory(private val psqlContainer: PSQLContainer) : TestDatabase {
        override val name = "R2DBC-${psqlContainer.dockerImage}"

        override suspend fun createDB(): ConnectionProviderFactory {
            val db = psqlContainer.preparePostgresDB()
            return R2dbcConnectionProviderFactory(
                ConnectionFactories.get("r2dbc:postgresql://test:test@${db.host}:${db.port}/${db.databaseName}"),
                db
            )
        }

    }


    inner class VertxPSQLTestDatabase(private val psql: PSQLContainer) : TestDatabase {
        override val name = "Vertx-${psql.dockerImage}"
        override suspend fun createDB(): ConnectionProviderFactory {
            val database = psql.preparePostgresDB()
            val connectOptions = PgConnectOptions()
                .setPort(database.port)
                .setHost(database.host)
                .setDatabase(database.databaseName)
                .setUser("test")
                .setPassword("test")

            return VertxConnectionProviderFactory(connectOptions, database)
        }

        override fun prepare() {
            psql.prepare()
        }
    }

}

class VertxConnectionProviderFactory(private val poolOptions: PgConnectOptions, private val db: AutoCloseable) :
    ConnectionProviderFactory {
    private val clients = mutableListOf<SqlClient>()
    override suspend fun create(): TransactionProvider {
        val client = PgPool.pool(poolOptions, PoolOptions().setMaxSize(TEST_POOL_SIZE))
        clients.add(client)
        val connectionProvider = TransactionalConnectionProvider(VertxDBConnectionFactory(client))
        connectionProvider.withConnection { it.execute(schemaSql) }
        return connectionProvider
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
        val dbConnectionFactory = R2DbcDBConnectionFactory(pool)
        val connectionProvider = TransactionalConnectionProvider(dbConnectionFactory)
        connectionProvider.withConnection { it.execute(schemaSql) }
        return connectionProvider
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
            val createDB by dependency({ db.createDB() }) { it.close() }
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
            val createDB by dependency({ testDB.createDB() }) { it.close() }
            val connectionFactory: suspend () -> TransactionProvider =
                { createDB.create() }
            tests(connectionFactory)
        }
    }
}

