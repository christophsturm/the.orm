package io.the.orm.test

import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.pgclient.PgBuilder
import io.vertx.pgclient.PgConnectOptions
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.PoolOptions
import kotlinx.coroutines.runBlocking
import org.testcontainers.containers.PostgreSQLContainer
import java.util.UUID
import kotlin.time.measureTimedValue

class LazyPSQLContainer(
    val dockerImage: String,
    private val databasePrefix: String,
    private val reuse: Boolean
) {
    fun prepare() {
        dockerContainer
    }

    val dockerContainer: PostgresqlContainer by lazy {
        PostgresqlContainer(dockerImage, databasePrefix, reuse)
    }

    suspend fun preparePostgresDB(): PostgresDb = dockerContainer.preparePostgresDB()
}

class PostgresqlContainer(
    dockerImage: String,
    private val databasePrefix: String,
    private val reuse: Boolean
) {
    private val dockerContainer: PostgreSQLContainer<Nothing> =
        measureTimedValue {
            PostgreSQLContainer<Nothing>(dockerImage).apply {
                setCommand("postgres", "-c", "fsync=off", "-c", "max_connections=20000")
                withReuse(reuse)
                start()
            }
        }.also { println("creating docker container took ${it.duration}") }.value
    private val vertx = Vertx.vertx()
    private val host = dockerContainer.host.let { if (it == "localhost") "127.0.0.1" else it }!!
    private val port = dockerContainer.getMappedPort(5432)!!
    private val connectOptions = PgConnectOptions()
        .setPort(port)
        .setHost(host)
        .setDatabase("postgres")
        .setUser("test")
        .setPassword("test")
    private val pool =
        PgBuilder.pool().with(PoolOptions().setMaxSize(5)).connectingTo(connectOptions).using(vertx).build()!!

    suspend fun preparePostgresDB(): PostgresDb = postgresDb(databasePrefix, port, host, pool)
}

internal suspend fun postgresDb(prefix: String, port: Int, host: String, pool: Pool): PostgresDb {
    val uuid = UUID.randomUUID().toString().take(5)
    val databaseName = "$prefix$uuid".replace("-", "_")
    val postgresDb = PostgresDb(databaseName, port, host, pool)
    postgresDb.createDb()
    return postgresDb
}

data class PostgresDb(val databaseName: String, val port: Int, val host: String, val pool: Pool) : AutoCloseable {
    suspend fun createDb() {
        Counters.createDatabase.add {
            executeSql("create database $databaseName")
        }
    }

    private suspend fun dropDb() {
        executeSql("drop database $databaseName")
    }

    private suspend fun executeSql(command: String) {
        pool.query(command).execute().coAwait()
    }

    override fun close() {
        runBlocking {
            dropDb()
        }
    }
}
