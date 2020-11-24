package r2dbcfun.transaction

import io.r2dbc.spi.Connection
import kotlinx.coroutines.reactive.awaitFirstOrNull

public suspend fun Connection.transaction(function: suspend () -> Unit) {
    this.beginTransaction().awaitFirstOrNull() // also disables auto-commit
    try {
        function()
    } catch (e: Exception) {
        this.rollbackTransaction().awaitFirstOrNull()
        throw e
    }
    this.commitTransaction().awaitFirstOrNull()
}
