package r2dbcfun

import io.r2dbc.spi.Result
import io.r2dbc.spi.Statement
import kotlinx.coroutines.reactive.awaitSingle

suspend fun Result.singleInt(): Int {
    return this.map { row, _ ->
        row.get(0, Integer::class.java)!!.toInt()
    }.awaitSingle()
}

suspend fun Statement.executeInsert() = this.returnGeneratedValues().execute().awaitSingle().singleInt()
