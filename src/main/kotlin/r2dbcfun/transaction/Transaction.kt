package r2dbcfun.transaction

import kotlinx.coroutines.reactive.awaitFirstOrNull
import r2dbcfun.r2dbc.DBConnection

suspend fun <T> transaction(connection: DBConnection, function: suspend () -> T): T {
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
