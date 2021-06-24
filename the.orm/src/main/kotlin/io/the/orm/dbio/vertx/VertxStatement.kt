package io.the.orm.dbio.vertx

import io.the.orm.dbio.DBResult
import io.the.orm.dbio.Statement
import io.vertx.kotlin.coroutines.await
import io.vertx.sqlclient.PreparedQuery
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class VertxStatement(private val preparedQuery: PreparedQuery<RowSet<Row>>) :
    Statement {
    override suspend fun execute(types: List<Class<*>>, values: Sequence<Any?>): DBResult {
        val rowSet = preparedQuery.execute(Tuple.from(values.toList())).await()
        return VertxResult(rowSet)
    }

    override suspend fun executeBatch(types: List<Class<*>>, valuesList: Sequence<Sequence<Any?>>): Flow<DBResult> {
        val list = valuesList.map { Tuple.from(it.toList()) }.toList()

        var rowSet = preparedQuery.executeBatch(list).await()
        return flow {
            while (true) {
                emit(VertxResult(rowSet))
                rowSet = rowSet.next() ?: break
            }

        }
    }
}
