package io.the.orm.test

import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.await
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import kotlinx.coroutines.runBlocking
import org.testcontainers.containers.PostgreSQLContainer
import java.util.UUID

class PSQLContainer(
    val dockerImage: String,
    private val databasePrefix: String,
    private val reuse: Boolean
) {
    fun prepare() {
        dockerContainer
    }

    private val dockerContainer: PostgreSQLContainer<Nothing> by
        lazy {
            PostgreSQLContainer<Nothing>(dockerImage).apply {
// WIP           setCommand("postgres", "-c", "fsync=off", "-c", "max_connections=200")
                withReuse(reuse)
                start()
            }
        }

    private val vertx = Vertx.vertx()
    private val host = dockerContainer.host.let { if (it == "localhost") "127.0.0.1" else it }!!
    private val port = dockerContainer.getMappedPort(5432)!!
    private val connectOptions = PgConnectOptions()
        .setPort(port)
        .setHost(host)
        .setDatabase("postgres")
        .setUser("test")
        .setPassword("test")
    private val pool = PgPool.pool(vertx, connectOptions, PoolOptions())!!

    suspend fun preparePostgresDB(): PostgresDb {
        val uuid = UUID.randomUUID().toString().take(5)
        val databaseName = "$databasePrefix$uuid".replace("-", "_")
        val postgresDb = PostgresDb(databaseName, port, host, pool)
        postgresDb.createDb()
        return postgresDb
    }
}

data class PostgresDb(val databaseName: String, val port: Int, val host: String, val pool: PgPool) : AutoCloseable {
    suspend fun createDb() {
        executeSql("create database $databaseName")
    }

    private suspend fun dropDb() {
        executeSql("drop database $databaseName")
    }

    private suspend fun executeSql(command: String) {
        pool.query(command).execute().await()
    }

    override fun close() {
        runBlocking {
            dropDb()
        }
    }
}
