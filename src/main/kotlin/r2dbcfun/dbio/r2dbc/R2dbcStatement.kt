package r2dbcfun.dbio.r2dbc

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

}
