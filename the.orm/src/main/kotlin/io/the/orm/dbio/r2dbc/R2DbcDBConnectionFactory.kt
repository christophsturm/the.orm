package io.the.orm.dbio.r2dbc

import io.r2dbc.pool.ConnectionPool
import io.the.orm.dbio.DBConnection
import io.the.orm.dbio.DBConnectionFactory
import kotlinx.coroutines.reactive.awaitSingle

class R2DbcDBConnectionFactory(private val connectionPool: ConnectionPool) : DBConnectionFactory {
    override suspend fun getConnection(): DBConnection {
        return R2dbcConnection(connectionPool.create().awaitSingle())
    }
}
