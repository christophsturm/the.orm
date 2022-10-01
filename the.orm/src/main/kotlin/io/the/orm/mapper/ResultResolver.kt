package io.the.orm.mapper

import io.the.orm.RepositoryException
import io.the.orm.dbio.DBResult
import io.the.orm.internal.classinfo.ClassInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class ResultResolver<Entity : Any>(private val classInfo: ClassInfo<Entity>) {
    suspend fun getResolvedValues(queryResult: DBResult): Flow<List<ResultPair>> {
        val parameters: Flow<List<ResultPair>> = queryResult.map { row ->
            classInfo.fieldInfo.map { fieldInfo ->
                val result = try {
                    row.getLazy(fieldInfo.dbFieldName)
                } catch (e: Exception) {
                    throw RepositoryException("error getting value for field ${fieldInfo.dbFieldName}", e)
                }
                LazyResultPair(
                    fieldInfo, try {
                        result
                    } catch (e: Exception) {
                        throw RepositoryException("error resolving value for field ${fieldInfo.dbFieldName}", e)
                    }
                )
            }
        }.map { it.map { ResultPair(it.fieldInfo, it.lazyResult.resolve()) } }
        return parameters
    }
}
