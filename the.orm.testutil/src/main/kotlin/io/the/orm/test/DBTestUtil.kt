package io.the.orm.test

import failgood.ContextDSL
import failgood.Ignored
import failgood.RootContext
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
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import kotlinx.coroutines.reactive.awaitFirstOrNull
import java.time.Duration
import java.util.UUID
import kotlin.reflect.KClass

object TestUtilConfig {
    val ALL_PSQL = System.getenv("ALL_PSQL") != null
    val H2_ONLY = System.getenv("H2_ONLY") != null
    const val TEST_POOL_SIZE = 2
}

class DBTestUtil(val databaseName: String) {
    val h2 = H2TestDatabase()
    val psql14 = PSQLContainer("postgres:14-alpine", databaseName, true)
    val psql14R2DBC = R2DBCPostgresFactory(psql14)
    val psql14Vertx = VertxPSQLTestDatabase(psql14)
    private val postgreSQLLegacyContainers = if (TestUtilConfig.ALL_PSQL) listOf(
        PSQLContainer("postgres:15beta4-alpine", databaseName, false),
        PSQLContainer("postgres:13-alpine", databaseName, false),
        PSQLContainer("postgres:12-alpine", databaseName, false),
        PSQLContainer("postgres:11-alpine", databaseName, false),
        PSQLContainer("postgres:10-alpine", databaseName, false),
        PSQLContainer("postgres:9-alpine", databaseName, false)
    )
    else listOf()

    val databases = if (TestUtilConfig.H2_ONLY) {
        listOf(h2)
    } else listOf(h2, psql14R2DBC, psql14Vertx) +
        postgreSQLLegacyContainers.flatMap { listOf(R2DBCPostgresFactory(it), VertxPSQLTestDatabase(it)) }

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
    private val pools = mutableListOf<PgPool>()
    override suspend fun create(): TransactionProvider {
        val client = PgPool.pool(poolOptions, PoolOptions().setMaxSize(TEST_POOL_SIZE))
        pools.add(client)

        return TransactionalConnectionProvider(VertxDBConnectionFactory(client))
    }

    override suspend fun close() {
        pools.forEach {
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
        return TransactionalConnectionProvider(dbConnectionFactory)
    }

    override suspend fun close() {
        val poolMetrics = buildString {
            pools.forEach {
                val metrics = it.metrics.get()
                append("before close: [allocatedSize: ${metrics.allocatedSize()}")
                append(" acquiredSize: ${metrics.acquiredSize()}]")
                it.disposeLater().awaitFirstOrNull()
                append("\nafter close:[allocatedSize: ${metrics.allocatedSize()}")
                append(" acquiredSize: ${metrics.acquiredSize()}")
                append(" disposed: ${it.isDisposed}]")
            }
        }
        try {
            closable?.close()
        } catch (e: Exception) {
            println("ERROR dropping database. pool metrics:$poolMetrics")
        }
    }
}

interface ConnectionProviderFactory {
    suspend fun create(): TransactionProvider
    suspend fun close()
}

suspend fun ContextDSL<*>.forAllDatabases(
    databases: List<DBTestUtil.TestDatabase>,
    schema: String,
    tests: suspend ContextDSL<*>.(suspend () -> TransactionProvider) -> Unit
) {
    databases.map { db ->
        withDb(db, schema, tests)
    }
}

suspend fun ContextDSL<*>.withDb(
    db: DBTestUtil.TestDatabase,
    schema: String,
    tests: suspend ContextDSL<*>.(suspend () -> TransactionProvider) -> Unit
) {
    context("on ${db.name}") {
        withDbInternal(db, schema, tests)
    }
}

private suspend fun ContextDSL<Unit>.withDbInternal(
    db: DBTestUtil.TestDatabase,
    schema: String?,
    tests: suspend ContextDSL<*>.(suspend () -> TransactionProvider) -> Unit
) {
    val createDB by dependency({ db.createDB() }) { it.close() }
    val connectionFactory: suspend () -> TransactionProvider =
        {
            val transactionProvider = createDB.create()
            if (schema == null) transactionProvider else transactionProvider.also { t ->
                t.withConnection { dbConnection ->
                    dbConnection.execute(schema)
                }
            }
        }
    tests(connectionFactory)
}
inline fun <reified Subject> describeOnAllDbs(
    databases: List<DBTestUtil.TestDatabase> = DBS.databases,
    schema: String? = null,
    ignored: Ignored? = null,
    noinline tests: suspend ContextDSL<*>.(suspend () -> TransactionProvider) -> Unit
) = describeOnAllDbs(Subject::class, databases, schema, ignored, tests)
fun describeOnAllDbs(
    subject: KClass<*>,
    databases: List<DBTestUtil.TestDatabase> = DBS.databases,
    schema: String? = null,
    ignored: Ignored? = null,
    tests: suspend ContextDSL<*>.(suspend () -> TransactionProvider) -> Unit
) = describeOnAllDbs("the ${subject.simpleName!!}", databases, schema, ignored, tests)

fun describeOnAllDbs(
    contextName: String,
    databases: List<DBTestUtil.TestDatabase> = DBS.databases,
    schema: String? = null,
    ignored: Ignored? = null,
    tests: suspend ContextDSL<*>.(suspend () -> TransactionProvider) -> Unit
): List<RootContext> {
    return databases.mapIndexed { index, testDB ->
        RootContext("$contextName (running on ${testDB.name})", ignored, order = index) {
            withDbInternal(testDB, schema, tests)
        }
    }
}
