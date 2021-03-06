package r2dbcfun

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import r2dbcfun.dbio.DBResult
import r2dbcfun.dbio.LazyResult
import r2dbcfun.internal.ClassCreator

internal class ResultMapper<T : Any>(
    private val table: String,
    private val classCreator: ClassCreator<T>
) {

    internal suspend fun mapQueryResult(queryResult: DBResult): Flow<T> {
        data class ResultPair(val fieldInfo: ClassCreator.FieldInfo, val result: LazyResult<Any?>)

        val parameters: Flow<List<ResultPair>> =
            queryResult
                .map { row ->
                    classCreator.fieldInfo
                        .map { entry ->
                            val result = row.getLazy(entry.snakeCaseName)
                            ResultPair(entry, result)
                        }
                }
        return parameters.map { values ->
            val resolvedParameters =
                values.associateTo(HashMap()) { (fieldInfo, result) ->
                    val resolvedValue = result.resolve()
                    val value = fieldInfo.fieldConverter.valueToConstructorParameter(resolvedValue)
                    Pair(fieldInfo.constructorParameter, value)
                }
            try {
                classCreator.constructor.callBy(resolvedParameters)
            } catch (e: IllegalArgumentException) {
                throw RepositoryException(
                    "error invoking constructor for $table. parameters:$resolvedParameters",
                    e
                )
            }
        }
    }
}
