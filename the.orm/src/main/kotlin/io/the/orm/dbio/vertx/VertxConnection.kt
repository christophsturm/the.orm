package io.the.orm.dbio.vertx

import io.the.orm.dbio.DBConnection
import io.the.orm.dbio.DBTransaction
import io.the.orm.dbio.Statement
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.SqlConnection

class VertxConnection(private val client: SqlConnection) : DBConnection {
    override fun createStatement(sql: String): Statement {
        return VertxStatement(client.preparedQuery(sql), sql)
    }

    override fun createInsertStatement(sql: String): Statement {
        return createStatement("$sql returning id")
    }

    override suspend fun beginTransaction(): DBTransaction {
        return VertxTransaction(client.begin().coAwait())
    }

    override suspend fun close() {
        client.close().coAwait()
    }

    override suspend fun execute(sql: String) {
        client.query(sql).execute().coAwait()
    }
}
