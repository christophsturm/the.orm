package io.the.orm.dbio.vertx

import io.the.orm.dbio.DBConnection
import io.the.orm.dbio.DBTransaction
import io.the.orm.dbio.Statement
import io.vertx.reactivex.sqlclient.SqlConnection
import kotlinx.coroutines.rx2.await

class VertxConnection(private val client: SqlConnection) : DBConnection {
    override fun createStatement(sql: String): Statement {
        return VertxStatement(client.preparedQuery(sql))
    }

    override fun createInsertStatement(sql: String): Statement {
        return createStatement("$sql returning id")
    }


    override suspend fun beginTransaction(): DBTransaction {
        return VertxTransaction(client.rxBegin().await())
    }

    override suspend fun close() {
        client.rxClose().await()
    }

    override suspend fun execute(sql: String) {
        client.query(sql).rxExecute().await()
    }

}
