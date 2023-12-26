package io.the.orm.dbio.r2dbc

import io.the.orm.OrmException
import io.the.orm.dbio.DBResult
import io.the.orm.dbio.Statement
import io.vertx.pgclient.PgException
import java.lang.IllegalArgumentException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle

class R2dbcStatement(private val statement: io.r2dbc.spi.Statement, private val sql: String) :
    Statement {
    override suspend fun execute(types: List<Class<*>>, values: List<Any?>): DBResult {
        values.forEachIndexed { index, o ->
            try {
                if (o == null) {
                    statement.bindNull(index, types[index])
                } else statement.bind(index, o)
            } catch (e: IllegalArgumentException) {
                throw OrmException(
                    "error binding parameter with value $o and index $index to statement $sql",
                    e
                )
            } catch (e: PgException) {
                throw OrmException(
                    "error binding parameter with value $o and index $index to statement $sql",
                    e
                )
            }
        }
        return R2dbcResult(statement.execute().awaitSingle())
    }

    override suspend fun executeBatch(
        types: List<Class<*>>,
        valuesList: List<List<Any?>>
    ): Flow<DBResult> {
        valuesList.forEachIndexed { valuesIdx, values ->
            if (valuesIdx != 0) statement.add()
            values.forEachIndexed { index, o ->
                val indexString = "$${index + 1}" // binding by index is broken in r2dbc-h2 0.9
                if (o == null) {
                    statement.bindNull(indexString, types[index])
                } else statement.bind(indexString, o)
            }
        }
        val result = statement.execute().asFlow()
        return result.map { R2dbcResult(it) }
    }
}
