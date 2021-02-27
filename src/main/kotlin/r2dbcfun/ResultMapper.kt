package r2dbcfun

import io.r2dbc.spi.Clob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow

internal class ResultMapper<T : Any>(
    private val table: String,
    private val classInfo: ClassInfo<T>
) {

    internal suspend fun mapQueryResult(queryResult: Result): Flow<T> {
        data class ResultPair(val fieldInfo: ClassInfo.FieldInfo, val result: Any?)

        val parameters: Flow<List<ResultPair>> =
            queryResult
                .map { row, _ ->
                    classInfo.fieldInfo
                        .map { entry ->
                            val result = row.get(entry.snakeCaseName)
                            ResultPair(entry, result)
                        }
                }
                .asFlow()
        return parameters.map { values ->
            val resolvedParameters =
                values.associateTo(HashMap()) { (fieldInfo, result) ->
                    val resolvedValue = when (result) {
                        is Clob -> resolveClob(result)
                        else -> result
                    }
                    val value = fieldInfo.fieldConverter.valueToConstructorParameter(resolvedValue)
                    Pair(fieldInfo.constructorParameter, value)
                }
            try {
                classInfo.constructor.callBy(resolvedParameters)
            } catch (e: IllegalArgumentException) {
                throw RepositoryException(
                    "error invoking constructor for $table. parameters:$resolvedParameters",
                    e
                )
            }
        }
    }

    private suspend fun resolveClob(result: Clob): String {
        val sb = StringBuilder()
        result.stream()
            .asFlow()
            .collect { chunk -> @Suppress("BlockingMethodInNonBlockingContext") sb.append(chunk) }
        result.discard()
        return sb.toString()
    }
}
