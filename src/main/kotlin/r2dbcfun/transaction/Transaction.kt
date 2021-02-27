package r2dbcfun.transaction

import io.r2dbc.spi.Connection
import kotlinx.coroutines.reactive.awaitFirstOrNull

suspend fun <T> transaction(connection: Connection, function: suspend () -> T): T {
    connection.beginTransaction().awaitFirstOrNull() // also disables auto-commit
    val result = try {
        function()
    } catch (e: Exception) {
        connection.rollbackTransaction().awaitFirstOrNull()
        throw e
    }
    connection.commitTransaction().awaitFirstOrNull()
    return result
}
