package r2dbcfun

import io.r2dbc.spi.Result
import io.r2dbc.spi.Statement
import kotlinx.coroutines.reactive.awaitSingle

suspend fun Result.singleLong(): Long {
    return this.map { row, _ ->
        row.get(0, java.lang.Long::class.java)!!.toLong()
    }.awaitSingle()
}

suspend fun Statement.executeInsert() = this.returnGeneratedValues().execute().awaitSingle().singleLong()
