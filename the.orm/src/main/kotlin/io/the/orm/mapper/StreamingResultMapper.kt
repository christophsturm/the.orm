package io.the.orm.mapper

import io.the.orm.RepositoryException
import io.the.orm.dbio.DBResult
import io.the.orm.dbio.LazyResult
import io.the.orm.internal.classinfo.ClassInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * This mapper does not support fetching relationships, but it avoids having to convert the flow to a list.
 */
internal class StreamingResultMapper<T : Any>(
    private val classInfo: ClassInfo<T>
) : ResultMapper<T> {

    override suspend fun mapQueryResult(queryResult: DBResult): Flow<T> {
        data class ResultPair(val fieldInfo: ClassInfo.FieldInfo, val result: LazyResult<Any?>)

        val parameters: Flow<List<ResultPair>> = queryResult.map { row ->
            classInfo.fieldInfo.map { entry ->
                val result = try {
                    row.getLazy(entry.dbFieldName)
                } catch (e: Exception) {
                    throw RepositoryException("error getting value for field ${entry.dbFieldName}", e)
                }
                ResultPair(entry, result)
            }
        }
        return parameters.map { values ->
            val resolvedParameters = values.associateTo(HashMap()) { (fieldInfo, result) ->
                val resolvedValue: Any? = try {
                    result.resolve()
                } catch (e: Exception) {
                    throw RepositoryException("error resolving value for field ${fieldInfo.dbFieldName}", e)
                }
                val value = fieldInfo.fieldConverter.dbValueToParameter(resolvedValue)
                Pair(fieldInfo.constructorParameter, value)
            }
            try {
                classInfo.constructor.callBy(resolvedParameters)
            } catch (e: IllegalArgumentException) {
                throw RepositoryException(
                    "error invoking constructor for ${classInfo.name}. parameters:$resolvedParameters", e
                )
            }
        }
    }
}
