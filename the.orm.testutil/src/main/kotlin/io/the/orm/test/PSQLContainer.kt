package io.the.orm.test

import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.await
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgConnection
import kotlinx.coroutines.runBlocking
import org.testcontainers.containers.PostgreSQLContainer
import java.util.UUID

class PSQLContainer(
    val dockerImage: String,
    private val databasePrefix: String = "r2dbc-testdatabase",
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

    suspend fun preparePostgresDB(): PostgresDb {
        val uuid = UUID.randomUUID()
        val databaseName = "$databasePrefix$uuid".replace("-", "_")
        // testcontainers says that it returns an ip address, but it returns a host name.
        val host = dockerContainer.host.let { if (it == "localhost") "127.0.0.1" else it }
        val port = dockerContainer.getMappedPort(5432)
        val postgresDb = PostgresDb(databaseName, host, port)
        postgresDb.createDb()
        return postgresDb
    }
}

data class PostgresDb(val databaseName: String, val host: String, val port: Int) : AutoCloseable {
    suspend fun createDb() {
        executeSql("create database $databaseName")
    }

    private suspend fun dropDb() {
        executeSql("drop database $databaseName")
    }

    private suspend fun executeSql(command: String) {
        val vertx = Vertx.vertx()
        val connection = PgConnection.connect(
            vertx, PgConnectOptions()
                .setPort(port)
                .setHost(host)
                .setDatabase("postgres")
                .setUser("test")
                .setPassword("test")
        ).await()
        try {
            connection.query(command).execute().await()
        } finally {
            connection.close().await()
            vertx.close()
        }
    }

    override fun close() {
        runBlocking {
            dropDb()
        }
    }
}
