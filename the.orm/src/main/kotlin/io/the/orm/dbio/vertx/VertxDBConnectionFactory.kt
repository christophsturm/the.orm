package io.the.orm.dbio.vertx

import io.the.orm.dbio.DBConnection
import io.the.orm.dbio.DBConnectionFactory
import io.vertx.reactivex.pgclient.PgPool
import kotlinx.coroutines.rx2.await

class VertxDBConnectionFactory(private val pool: PgPool) : DBConnectionFactory {
    override suspend fun getConnection(): DBConnection {
        return VertxConnection(pool.rxGetConnection().await())
    }

}
