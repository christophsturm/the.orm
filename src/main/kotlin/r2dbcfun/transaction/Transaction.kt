package r2dbcfun.transaction

import io.r2dbc.spi.Connection
import kotlinx.coroutines.reactive.awaitFirstOrNull

public suspend fun Connection.transaction(function: suspend () -> Unit) {
    this.beginTransaction().awaitFirstOrNull() // also disables auto-commit
    function()
    this.commitTransaction().awaitFirstOrNull()
}
