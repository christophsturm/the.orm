package r2dbcfun.dbio.vertx

import io.vertx.reactivex.sqlclient.PreparedQuery
import io.vertx.reactivex.sqlclient.Row
import io.vertx.reactivex.sqlclient.RowSet
import io.vertx.reactivex.sqlclient.Tuple
import kotlinx.coroutines.rx2.await
import r2dbcfun.dbio.DBResult
import r2dbcfun.dbio.Statement

class VertxStatement(private val preparedQuery: PreparedQuery<RowSet<Row>>) :
    Statement {
    override suspend fun execute(types: List<Class<*>>, values: Sequence<Any?>): DBResult {
        val rowSet = preparedQuery.rxExecute(Tuple.from(values.toList())).await()
        return VertxResult(rowSet)
    }

    override suspend fun executeBatch(types: List<Class<*>>, valuesList: Sequence<Sequence<Any?>>): DBResult {
        val rowSet = preparedQuery.rxExecuteBatch(valuesList.map { Tuple.from(it.toList()) }.toList()).await()
        return VertxResult(rowSet)
    }
}
