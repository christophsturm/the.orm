package r2dbcfun.dbio.vertx

import io.vertx.reactivex.pgclient.PgPool
import kotlinx.coroutines.rx2.await
import r2dbcfun.dbio.ConnectionFactory
import r2dbcfun.dbio.DBConnection

class VertxConnectionFactory(private val pool: PgPool) : ConnectionFactory {
    override suspend fun getConnection(): DBConnection {
        return VertxConnection(pool.rxGetConnection().await())
    }

}
