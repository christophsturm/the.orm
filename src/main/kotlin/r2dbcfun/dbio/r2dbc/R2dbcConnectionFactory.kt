package r2dbcfun.dbio.r2dbc

import io.r2dbc.pool.ConnectionPool
import kotlinx.coroutines.reactive.awaitSingle
import r2dbcfun.dbio.ConnectionFactory
import r2dbcfun.dbio.DBConnection

class R2dbcConnectionFactory(private val connectionPool: ConnectionPool) : ConnectionFactory {
    override suspend fun getConnection(): DBConnection {
        return R2dbcConnection(connectionPool.create().awaitSingle())
    }


}
