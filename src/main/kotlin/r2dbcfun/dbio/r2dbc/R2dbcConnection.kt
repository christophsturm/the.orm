package r2dbcfun.dbio.r2dbc

import kotlinx.coroutines.reactive.awaitFirstOrNull
import r2dbcfun.dbio.DBConnection
import r2dbcfun.dbio.Statement

class R2dbcConnection(val connection: io.r2dbc.spi.Connection) : DBConnection {

    override suspend fun beginTransaction() {
        connection.beginTransaction().awaitFirstOrNull()
    }

    override suspend fun commitTransaction() {
        connection.commitTransaction().awaitFirstOrNull()
    }

    override suspend fun rollbackTransaction() {
        connection.rollbackTransaction().awaitFirstOrNull()
    }

    override suspend fun close() {
        connection.close().awaitFirstOrNull()
    }

    override fun createStatement(sql: String): Statement {
        return R2dbcStatement(connection.createStatement(sql))
    }

    override fun createInsertStatement(sql: String): Statement {
        return R2dbcStatement(connection.createStatement(sql).returnGeneratedValues())
    }

}
