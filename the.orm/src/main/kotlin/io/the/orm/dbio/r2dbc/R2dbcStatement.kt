package io.the.orm.dbio.r2dbc

import io.the.orm.dbio.DBResult
import io.the.orm.dbio.Statement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle

class R2dbcStatement(private val statement: io.r2dbc.spi.Statement) : Statement {
    override suspend fun execute(types: List<Class<*>>, values: Sequence<Any?>): DBResult {
        values.forEachIndexed { index, o ->
            if (o == null) {
                statement.bindNull(index, types[index])
            } else
                statement.bind(index, o)
        }
        return R2dbcResult(statement.execute().awaitSingle())
    }

    override suspend fun executeBatch(types: List<Class<*>>, valuesList: Sequence<Sequence<Any?>>): Flow<DBResult> {
        valuesList.forEachIndexed { valuesIdx, values ->
            if (valuesIdx != 0)
                statement.add()
            values.forEachIndexed { index, o ->
                val indexString = "$${index + 1}" // binding by index is broken in r2dbc-h2 0.9
                if (o == null) {
                    statement.bindNull(indexString, types[index])
                } else
                    statement.bind(indexString, o)
            }
        }
        val result = statement.execute().asFlow()
        return result.map { R2dbcResult(it) }
    }

}
