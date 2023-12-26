package io.the.orm.dbio.vertx

import io.the.orm.OrmException
import io.the.orm.dbio.DBResult
import io.the.orm.dbio.Statement
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.pgclient.PgException
import io.vertx.sqlclient.PreparedQuery
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class VertxStatement(
    private val preparedQuery: PreparedQuery<RowSet<Row>>,
    private val sql: String
) : Statement {
    override suspend fun execute(types: List<Class<*>>, values: List<Any?>): DBResult {
        val parameterList = values.toList()
        val rowSet =
            try {
                preparedQuery.execute(Tuple.from(parameterList)).coAwait()
            } catch (e: PgException) {
                if (e.errorMessage.contains("unique"))
                    throw e // don't wrap the exception if it's about unique constraints because we
                // catch that later
                throw OrmException("error executing query $sql with parameters $parameterList", e)
            }
        return VertxResult(rowSet)
    }

    override suspend fun executeBatch(
        types: List<Class<*>>,
        valuesList: List<List<Any?>>
    ): Flow<DBResult> {
        val list = valuesList.map { Tuple.from(it.toList()) }.toList()

        var rowSet = preparedQuery.executeBatch(list).coAwait()
        return flow {
            while (true) {
                emit(VertxResult(rowSet))
                rowSet = rowSet.next() ?: break
            }
        }
    }
}
