package io.the.orm.dbio.r2dbc

import io.r2dbc.spi.Connection
import io.the.orm.dbio.DBConnection
import io.the.orm.dbio.DBTransaction
import io.the.orm.dbio.Statement
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitLast

class R2dbcConnection(val connection: Connection) : DBConnection {

    override suspend fun beginTransaction(): DBTransaction {
        connection.beginTransaction().awaitFirstOrNull()
        return R2DCTransaction(connection)
    }

    override suspend fun close() {
        connection.close().awaitFirstOrNull()
    }

    override suspend fun execute(sql: String) {
        connection.createStatement(sql).execute().awaitLast()
    }

    override fun createStatement(sql: String): Statement {
        return R2dbcStatement(connection.createStatement(sql), sql)
    }

    override fun createInsertStatement(sql: String): Statement {
        return R2dbcStatement(connection.createStatement(sql).returnGeneratedValues(), sql)
    }
}

class R2DCTransaction(val connection: Connection) : DBTransaction {
    override suspend fun commitTransaction() {
        connection.commitTransaction().awaitFirstOrNull()
    }

    override suspend fun rollbackTransaction() {
        connection.rollbackTransaction().awaitFirstOrNull()
    }
}
