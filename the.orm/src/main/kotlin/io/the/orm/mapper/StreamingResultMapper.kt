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
        data class LazyResultPair(val fieldInfo: ClassInfo.FieldInfo, val lazyResult: LazyResult<Any?>)
        data class ResultPair(val fieldInfo: ClassInfo.FieldInfo, val result: Any?)

        val parameters: Flow<List<ResultPair>> = queryResult.map { row ->
            classInfo.fieldInfo.map { fieldInfo ->
                val result = try {
                    row.getLazy(fieldInfo.dbFieldName)
                } catch (e: Exception) {
                    throw RepositoryException("error getting value for field ${fieldInfo.dbFieldName}", e)
                }
                LazyResultPair(fieldInfo, try {
                    result
                } catch (e: Exception) {
                        throw RepositoryException("error resolving value for field ${fieldInfo.dbFieldName}", e)
                    }
                )
            }
        }.map { it.map { ResultPair(it.fieldInfo, it.lazyResult.resolve()) } }
        return parameters.map { values ->
            values.associateTo(HashMap()) { (fieldInfo, result) ->
                val value = fieldInfo.fieldConverter.dbValueToParameter(result)
                Pair(fieldInfo.constructorParameter, value)
            }
        }.map {
            try {
                classInfo.constructor.callBy(it)
            } catch (e: IllegalArgumentException) {
                throw RepositoryException(
                    "error invoking constructor for ${classInfo.name}. parameters:$it", e
                )
            }
        }
    }
}
