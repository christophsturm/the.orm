package r2dbcfun.transaction

import r2dbcfun.dbio.r2dbc.DBConnection

suspend fun <T> transaction(connection: DBConnection, function: suspend () -> T): T {
    connection.beginTransaction()
    val result = try {
        function()
    } catch (e: Exception) {
        connection.rollbackTransaction()
        throw e
    }
    connection.commitTransaction()
    return result
}
