package io.the.orm.test

import org.testcontainers.containers.PostgreSQLContainer
import java.sql.DriverManager
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


    fun preparePostgresDB(): PostgresDb {
        Class.forName("org.postgresql.Driver")
        val uuid = UUID.randomUUID()
        val databaseName = "$databasePrefix$uuid".replace("-", "_")
        // testcontainers says that it returns an ip address but it returns a host name.
        val host = dockerContainer.containerIpAddress.let { if (it == "localhost") "127.0.0.1" else it }
        val port = dockerContainer.getMappedPort(5432)
        val postgresDb = PostgresDb(databaseName, host, port)
        postgresDb.createDb()
        return postgresDb
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
