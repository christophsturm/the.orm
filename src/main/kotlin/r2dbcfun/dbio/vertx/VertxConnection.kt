package r2dbcfun.dbio.vertx

import io.vertx.reactivex.sqlclient.SqlConnection
import kotlinx.coroutines.rx2.await
import r2dbcfun.dbio.DBConnection
import r2dbcfun.dbio.DBTransaction
import r2dbcfun.dbio.Statement

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

}
