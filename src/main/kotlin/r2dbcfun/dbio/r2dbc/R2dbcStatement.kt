package r2dbcfun.dbio.r2dbc

import kotlinx.coroutines.reactive.awaitSingle
import r2dbcfun.dbio.DBResult
import r2dbcfun.dbio.Statement

class R2dbcStatement(private val statement: io.r2dbc.spi.Statement) : Statement {
    override fun bind(idx: Int, property: Any): Statement {
        statement.bind(idx, property)
        return this
    }

    override fun bind(field: String, property: Any): Statement {
        statement.bind(field, property)
        return this
    }

    override suspend fun execute(): DBResult {
        return R2dbcResult(statement.execute().awaitSingle())
    }

    override fun bindNull(index: Int, dbClass: Class<out Any>): Statement {
        statement.bindNull(index, dbClass)
        return this
    }

    suspend fun executeInsert(): Long {
        return statement.returnGeneratedValues().execute().awaitSingle()
            .map { row, _ -> row.get(0, java.lang.Long::class.java)!!.toLong() }.awaitSingle()
    }

    override suspend fun executeInsert(types: List<Class<*>>, values: Sequence<Any?>): Long {
        values.forEachIndexed { index, o ->
            if (o == null) {
                statement.bindNull(index, types[index])
            } else
                statement.bind(index, o)
        }
        return executeInsert()
    }

}
