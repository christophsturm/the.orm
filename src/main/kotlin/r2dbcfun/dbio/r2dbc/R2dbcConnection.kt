package r2dbcfun.dbio.r2dbc

import kotlinx.coroutines.reactive.awaitFirstOrNull
import r2dbcfun.dbio.DBConnection
import r2dbcfun.dbio.Statement

class R2dbcConnection(val connection: io.r2dbc.spi.Connection) : DBConnection {

    override fun createStatement(sql: String): Statement {
        return R2dbcStatement(connection.createStatement(sql))
    }

    override fun createInsertStatement(sql: String): Statement {
        return R2dbcStatement(connection.createStatement(sql).returnGeneratedValues())
    }
    override suspend fun <T> transaction(function: suspend () -> T): T {
        connection.beginTransaction().awaitFirstOrNull()
        val result = try {
            function()
        } catch (e: Exception) {
            connection.rollbackTransaction().awaitFirstOrNull()
            throw e
        }
        connection.commitTransaction().awaitFirstOrNull()
        return result
    }

}
