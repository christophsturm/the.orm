package r2dbcfun

import io.r2dbc.spi.R2dbcException
import io.r2dbc.spi.Result
import kotlinx.coroutines.reactive.awaitSingle
import r2dbcfun.transaction.transaction

class ConnectionProvider(val connection: Connection) {
    constructor(connection: io.r2dbc.spi.Connection) : this(Connection(connection))

    suspend fun <T> transaction(function: suspend () -> T): T = connection.transaction(function)
}

class Connection(val connection: io.r2dbc.spi.Connection) : io.r2dbc.spi.Connection by connection {
    suspend fun executeSelect(
        parameterValues: Sequence<Any>,
        sql: String
    ): Result {
        val statement = try {
            parameterValues.foldIndexed(createStatement(sql))
            { idx, statement, property -> statement.bind(idx, property) }
        } catch (e: R2dbcException) {
            throw RepositoryException("error creating statement for sql:$sql", e)
        }
        return try {
            statement.execute().awaitSingle()
        } catch (e: R2dbcException) {
            throw RepositoryException("error executing select: $sql", e)
        }
    }

}
