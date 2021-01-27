package r2dbcfun.transaction

import io.r2dbc.spi.Connection
import kotlinx.coroutines.reactive.awaitFirstOrNull

public suspend fun <T> Connection.transaction(function: suspend () -> T): T {
    this.beginTransaction().awaitFirstOrNull() // also disables auto-commit
    val result = try {
        function()
    } catch (e: Exception) {
        this.rollbackTransaction().awaitFirstOrNull()
        throw e
    }
    this.commitTransaction().awaitFirstOrNull()
    return result
}
