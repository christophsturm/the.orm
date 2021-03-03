package r2dbcfun.dbio.r2dbc

import io.r2dbc.pool.ConnectionPool
import kotlinx.coroutines.reactive.awaitSingle
import r2dbcfun.dbio.DBConnection
import r2dbcfun.dbio.DBConnectionFactory

class R2DbcDBConnectionFactory(private val connectionPool: ConnectionPool) : DBConnectionFactory {
    override suspend fun getConnection(): DBConnection {
        return R2dbcConnection(connectionPool.create().awaitSingle())
    }


}
