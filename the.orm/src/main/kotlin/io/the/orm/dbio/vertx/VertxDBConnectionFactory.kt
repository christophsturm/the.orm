package io.the.orm.dbio.vertx

import io.the.orm.dbio.DBConnection
import io.the.orm.dbio.DBConnectionFactory
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.Pool

class VertxDBConnectionFactory(private val pool: Pool) : DBConnectionFactory {
    override suspend fun getConnection(): DBConnection {
        return VertxConnection(pool.connection.coAwait())
    }
}
