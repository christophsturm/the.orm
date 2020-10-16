package r2dbcfun

import io.r2dbc.spi.Result
import io.r2dbc.spi.Statement
import kotlinx.coroutines.reactive.awaitSingle
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

internal suspend fun Result.singleLong(): Long {
    return this.map { row, _ ->
        row.get(0, java.lang.Long::class.java)!!.toLong()
    }.awaitSingle()
}

internal suspend fun Statement.executeInsert() = this.returnGeneratedValues().execute().awaitSingle().singleLong()
internal fun bindValueOrNull(
    statement: Statement,
    index: Int,
    value: Any?,
    kClass: KClass<*>,
    fieldName: String
): Statement {
    return try {
        if (value == null) {
            val clazz = if (kClass.isSubclassOf(Enum::class)) String::class.java else kClass.java
            statement.bindNull(index, clazz)
        } else {

            statement.bind(index, if (value::class.isSubclassOf(Enum::class)) value.toString() else value)
        }
    } catch (e: java.lang.IllegalArgumentException) {
        throw R2dbcRepoException(
            "error binding value $value to field $fieldName with index $index",
            e
        )
    }
}
