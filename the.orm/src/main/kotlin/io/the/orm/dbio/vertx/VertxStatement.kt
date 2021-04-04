package io.the.orm.dbio.vertx

import io.the.orm.dbio.DBResult
import io.the.orm.dbio.Statement
import io.vertx.reactivex.sqlclient.PreparedQuery
import io.vertx.reactivex.sqlclient.Row
import io.vertx.reactivex.sqlclient.RowSet
import io.vertx.reactivex.sqlclient.Tuple
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.rx2.await

class VertxStatement(private val preparedQuery: PreparedQuery<RowSet<Row>>) :
    Statement {
    override suspend fun execute(types: List<Class<*>>, values: Sequence<Any?>): DBResult {
        val rowSet = preparedQuery.rxExecute(Tuple.from(values.toList())).await()
        return VertxResult(rowSet)
    }

    override suspend fun executeBatch(types: List<Class<*>>, valuesList: Sequence<Sequence<Any?>>): Flow<DBResult> {
        val list = valuesList.map { Tuple.from(it.toList()) }.toList()

        var rowSet = preparedQuery.rxExecuteBatch(list).await()
        return flow {
            while (true) {
                emit(VertxResult(rowSet))
                rowSet = rowSet.next() ?: break
            }

        }
    }
}
