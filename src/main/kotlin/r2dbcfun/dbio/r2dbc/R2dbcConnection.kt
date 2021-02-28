package r2dbcfun.dbio.r2dbc

import io.r2dbc.spi.R2dbcException
import kotlinx.coroutines.reactive.awaitFirstOrNull
import r2dbcfun.RepositoryException
import r2dbcfun.dbio.DBConnection
import r2dbcfun.dbio.DBResult
import r2dbcfun.dbio.Statement

class R2dbcConnection(val connection: io.r2dbc.spi.Connection) : DBConnection {
    override suspend fun executeSelect(
        parameterValues: Sequence<Any>,
        sql: String
    ): DBResult {
        val statement = try {
            parameterValues.foldIndexed(createStatement(sql))
            { idx, statement, property -> statement.bind(idx, property) }
        } catch (e: R2dbcException) {
            throw RepositoryException("error creating statement for sql:$sql", e)
        }
        return try {
            statement.execute()
        } catch (e: R2dbcException) {
            throw RepositoryException("error executing select: $sql", e)
        }
    }

    override suspend fun beginTransaction() {
        connection.beginTransaction().awaitFirstOrNull()
    }

    override suspend fun commitTransaction() {
        connection.commitTransaction().awaitFirstOrNull()
    }

    override fun createStatement(sql: String): Statement {
        return R2dbcStatement(connection.createStatement(sql))
    }

    override suspend fun rollbackTransaction() {
        connection.rollbackTransaction().awaitFirstOrNull()
    }
}
