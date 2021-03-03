package r2dbcfun.dbio.vertx

import io.vertx.reactivex.pgclient.PgPool
import kotlinx.coroutines.rx2.await
import r2dbcfun.dbio.DBConnection
import r2dbcfun.dbio.DBConnectionFactory

class VertxDBConnectionFactory(private val pool: PgPool) : DBConnectionFactory {
    override suspend fun getConnection(): DBConnection {
        return VertxConnection(pool.rxGetConnection().await())
    }

}
