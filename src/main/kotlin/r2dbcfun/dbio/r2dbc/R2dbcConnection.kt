package r2dbcfun.dbio.r2dbc

import io.r2dbc.spi.Connection
import kotlinx.coroutines.reactive.awaitFirstOrNull
import r2dbcfun.dbio.DBConnection
import r2dbcfun.dbio.DBTransaction
import r2dbcfun.dbio.Statement

class R2dbcConnection(val connection: io.r2dbc.spi.Connection) : DBConnection {

    override suspend fun beginTransaction(): DBTransaction {
        connection.beginTransaction().awaitFirstOrNull()
        return R2DCTransaction(connection)
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

class R2DCTransaction(val connection: Connection) : DBTransaction {
    override suspend fun commitTransaction() {
        connection.commitTransaction().awaitFirstOrNull()
    }

    override suspend fun rollbackTransaction() {
        connection.rollbackTransaction().awaitFirstOrNull()
    }

}
