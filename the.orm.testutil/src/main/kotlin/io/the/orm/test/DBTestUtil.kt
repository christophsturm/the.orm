package io.the.orm.test

import failgood.Ignored
import failgood.RootContext
import failgood.describe
import failgood.dsl.ContextDSL
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.the.orm.dbio.DBConnection
import io.the.orm.dbio.DBConnectionFactory
import io.the.orm.dbio.TransactionProvider
import io.the.orm.dbio.TransactionalConnectionProvider
import io.the.orm.dbio.r2dbc.R2DbcDBConnectionFactory
import io.the.orm.dbio.vertx.VertxDBConnectionFactory
import io.the.orm.test.TestUtilConfig.TEST_POOL_SIZE
import io.vertx.pgclient.PgBuilder
import io.vertx.pgclient.PgConnectOptions
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlConnectOptions
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.util.UUID
import kotlin.reflect.KClass

object TestUtilConfig {
    val ALL_PSQL = System.getenv("ALL_PSQL") != null
    val H2_ONLY = System.getenv("H2_ONLY") != null
    val VERTX_ONLY = System.getenv("VERTX_ONLY") != null
    val LOCAL_VERTX_ONLY = System.getenv("LOCAL_VERTX_ONLY") != null
    const val TEST_POOL_SIZE = 2
}

class DBTestUtil(val databasePrefix: String) {
    private val h2 = H2TestDatabase()
    val psql16 = LazyPSQLContainer("postgres:16-alpine", databasePrefix, true)
    private val psql16R2DBC = R2DBCPostgresFactory(psql16)
    private val psql16Vertx = VertxPSQLTestDatabase(psql16)
    private val postgreSQLLegacyContainers = if (TestUtilConfig.ALL_PSQL) listOf(
        LazyPSQLContainer("postgres:15-alpine", databasePrefix, false),
        LazyPSQLContainer("postgres:14-alpine", databasePrefix, false),
        LazyPSQLContainer("postgres:13-alpine", databasePrefix, false),
        LazyPSQLContainer("postgres:12-alpine", databasePrefix, false),
        LazyPSQLContainer("postgres:11-alpine", databasePrefix, false),
        LazyPSQLContainer("postgres:10-alpine", databasePrefix, false),
        LazyPSQLContainer("postgres:9-alpine", databasePrefix, false)
    )
    else listOf()

    val databases: List<TestDatabase> = if (TestUtilConfig.H2_ONLY) {
        listOf(h2)
    } else if (TestUtilConfig.LOCAL_VERTX_ONLY) listOf(
        VertxLocalPsqlTestDatabase(
            databasePrefix,
            5432,
            "localhost"
        )
    ) else (if (TestUtilConfig.VERTX_ONLY) listOf(psql16Vertx) else listOf(h2, psql16R2DBC, psql16Vertx)) +
        postgreSQLLegacyContainers.flatMap {
            if (TestUtilConfig.VERTX_ONLY) listOf(VertxPSQLTestDatabase(it)) else listOf(
                R2DBCPostgresFactory(it),
                VertxPSQLTestDatabase(it)
            )
        }

    @Suppress("unused")
    val unstableDatabases: List<TestDatabase> = listOf()

    interface TestDatabase {
        val driverType: DriverType
        val name: String

        suspend fun createDB(): ConnectionProviderFactory
        fun prepare() {}
    }

    inner class H2TestDatabase : TestDatabase {
        override val driverType = DriverType.H2

        override val name = "H2"

        override suspend fun createDB(): ConnectionProviderFactory {
            val uuid = UUID.randomUUID()
            val databaseName = "$databasePrefix$uuid"
            val connectionFactory = ConnectionFactories.get("r2dbc:h2:mem:///$databaseName;DB_CLOSE_DELAY=-1")
            return R2dbcConnectionProviderFactory(connectionFactory)
        }
    }

    class R2DBCPostgresFactory(val psqlContainer: LazyPSQLContainer) : TestDatabase {
        override val driverType: DriverType = DriverType.R2DBC

        override val name = "R2DBC-${psqlContainer.dockerImage}"

        override suspend fun createDB(): ConnectionProviderFactory {
            val db = psqlContainer.preparePostgresDB()
            return R2dbcConnectionProviderFactory(
                ConnectionFactories.get("r2dbc:postgresql://test:test@${db.host}:${db.port}/${db.databaseName}"),
                db
            )
        }
    }

    inner class VertxPSQLTestDatabase(val psql: LazyPSQLContainer) : TestDatabase {
        override val driverType: DriverType = DriverType.VERTX
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

    /** vertx on a locally running database, without docker */
    inner class VertxLocalPsqlTestDatabase(
        private val databasePrefix: String,
        private val port: Int,
        private val host: String
    ) : TestDatabase {
        override val driverType: DriverType = DriverType.VERTX

        private val connectOptions = PgConnectOptions()
            .setPort(port)
            .setHost(host)
            .setDatabase("postgres")
            .setUser("postgres")
        private val pool = PgBuilder.pool().with(PoolOptions().setMaxSize(5)).connectingTo(connectOptions).build()!!
        override val name = "Vertx-local"
        suspend fun preparePostgresDB(): PostgresDb = postgresDb(databasePrefix, port, host, pool)

        override suspend fun createDB(): ConnectionProviderFactory {
            val database = preparePostgresDB()
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

enum class DriverType {
    H2, VERTX, R2DBC
}

class VertxConnectionProviderFactory(private val poolOptions: SqlConnectOptions, private val db: AutoCloseable) :
    ConnectionProviderFactory {
    private val pools = mutableListOf<Pool>()
    override suspend fun create(): DBConnectionFactory {
        val client = PgBuilder.pool().with(PoolOptions().setMaxSize(5)).connectingTo(poolOptions).build()!!
        pools.add(client)

        return VertxDBConnectionFactory(client)
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
    override suspend fun create(): DBConnectionFactory {
        val pool = ConnectionPool(
            ConnectionPoolConfiguration.builder(connectionFactory)
                .maxIdleTime(Duration.ofMillis(1000))
                .maxSize(TEST_POOL_SIZE)
                .build()
        )
        pools.add(pool)
        return R2DbcDBConnectionFactory(pool)
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
    suspend fun create(): DBConnectionFactory
    suspend fun close()
}

suspend fun ContextDSL<*>.forAllDatabases(
    databases: List<DBTestUtil.TestDatabase>,
    schema: String,
    tests: suspend ContextDSL<*>.(TransactionProvider) -> Unit
) {
    databases.map { db ->
        withDb(db, schema, tests)
    }
}

suspend fun ContextDSL<*>.withDb(
    db: DBTestUtil.TestDatabase,
    schema: String,
    tests: suspend ContextDSL<*>.(TransactionProvider) -> Unit
) {
    context("on ${db.name}") {
        withDbInternal(db, schema, tests)
    }
}

suspend fun ContextDSL<Unit>.withDbInternal(
    db: DBTestUtil.TestDatabase,
    schema: String?,
    tests: suspend ContextDSL<*>.(TransactionProvider) -> Unit
) {
    val createDB by dependency({ db.createDB() }) { it.close() }
    val dbConnection: DBConnectionFactory = LazyDBConnectionFactory(createDB, schema)

    tests(TransactionalConnectionProvider(dbConnection))
}

suspend fun DBTestUtil.TestDatabase.fixture(schema: String): TestDatabaseFixture {
    val connectionProviderFactory = createDB()
    val factory = connectionProviderFactory.create()
    factory.createSchema(schema)
    return TestDatabaseFixture(factory, connectionProviderFactory)
}

class TestDatabaseFixture(
    factory: DBConnectionFactory,
    private val connectionProviderFactory: ConnectionProviderFactory
) :
    AutoCloseable {
    val transactionProvider: TransactionProvider = TransactionalConnectionProvider(factory)
    override fun close() {
        runBlocking {
            connectionProviderFactory.close()
        }
    }
}

/*
 speed up the tests by creating the database as late as possible, when it is first accessed.
 */
class LazyDBConnectionFactory(
    private val db: ConnectionProviderFactory,
    private val schema: String?
) :
    DBConnectionFactory {
    private var factory: DBConnectionFactory? = null
    override suspend fun getConnection(): DBConnection {
        if (factory != null)
            return factory!!.getConnection()

        val dbConnectionFactory = db.create()
        if (schema != null) dbConnectionFactory.createSchema(schema)
        factory = dbConnectionFactory
        return dbConnectionFactory.getConnection()
    }
}

suspend fun DBConnectionFactory.createSchema(schema: String) {
    TransactionalConnectionProvider(this).withConnection { dbConnection ->
        Counters.createSchema.add {
            dbConnection.execute(schema)
        }
    }
}

inline fun <reified Subject> describeOnAllDbs(
    databases: List<DBTestUtil.TestDatabase> = DBS.databases,
    schema: String? = null,
    ignored: Ignored? = null,
    noinline tests: suspend ContextDSL<*>.(TransactionProvider) -> Unit
) = describeOnAllDbs(Subject::class, databases, schema, ignored, tests)

fun describeOnAllDbs(
    subject: KClass<*>,
    databases: List<DBTestUtil.TestDatabase> = DBS.databases,
    schema: String? = null,
    ignored: Ignored? = null,
    tests: suspend ContextDSL<*>.(TransactionProvider) -> Unit
) = describeOnAllDbs("the ${subject.simpleName!!}", databases, schema, ignored, tests)

fun describeOnAllDbs(
    contextName: String,
    databases: List<DBTestUtil.TestDatabase> = DBS.databases,
    schema: String? = null,
    ignored: Ignored? = null,
    tests: suspend ContextDSL<*>.(TransactionProvider) -> Unit
): List<RootContext> {
    return databases.mapIndexed { index, testDB ->
        val subjectDescription = if (databases.size == 1) contextName else "$contextName (running on ${testDB.name})"
        describe(subjectDescription, ignored, order = index) {
            withDbInternal(testDB, schema, tests)
        }
    }
}
