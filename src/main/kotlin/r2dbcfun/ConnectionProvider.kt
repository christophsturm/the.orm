package r2dbcfun

import io.r2dbc.spi.Connection
import r2dbcfun.transaction.transaction

class ConnectionProvider(val connection: Connection) {
    suspend fun <T> transaction(function: suspend () -> T): T = connection.transaction(function)
}
