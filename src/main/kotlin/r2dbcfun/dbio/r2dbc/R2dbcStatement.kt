package r2dbcfun.dbio.r2dbc

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import r2dbcfun.dbio.DBResult
import r2dbcfun.dbio.Statement

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
        valuesList.forEach { values ->
            values.forEachIndexed { index, o ->
                if (o == null) {
                    statement.bindNull(index, types[index])
                } else
                    statement.bind(index, o)
            }
            statement.add()
        }
        return statement.execute().asFlow().map { R2dbcResult(it) }
    }

}
