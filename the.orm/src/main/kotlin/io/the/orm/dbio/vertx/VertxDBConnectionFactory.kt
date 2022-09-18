package io.the.orm.dbio.vertx

import io.the.orm.dbio.DBConnection
import io.the.orm.dbio.DBConnectionFactory
import io.vertx.kotlin.coroutines.await
import io.vertx.pgclient.PgPool

class VertxDBConnectionFactory(private val pool: PgPool) : DBConnectionFactory {
    override suspend fun getConnection(): DBConnection {
        return VertxConnection(pool.connection.await())
    }
}
