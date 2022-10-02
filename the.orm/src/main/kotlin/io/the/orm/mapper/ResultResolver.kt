package io.the.orm.mapper

import io.the.orm.RepositoryException
import io.the.orm.dbio.DBResult
import io.the.orm.dbio.DBRow
import io.the.orm.internal.classinfo.ClassInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class ResultResolver<Entity : Any>(private val classInfo: ClassInfo<Entity>) {
    suspend fun getResolvedValues(queryResult: DBResult): Flow<ResultLine> {
        val map = queryResult.map { row ->
            LazyResultLine(classInfo.fields.map { fieldInfo ->
                lazyResultPair(row, fieldInfo)
            },
                classInfo.relations.map { fieldInfo ->
                    lazyResultPair(row, fieldInfo)
                }

            )
        }

        return map.map {
            ResultLine(
                it.fields.map { ResultPair(it.fieldInfo, it.lazyResult.resolve()) },
                it.relations.map { ResultPair(it.fieldInfo, it.lazyResult.resolve()) })
        }
    }

    private fun lazyResultPair(
        row: DBRow,
        fieldInfo: ClassInfo.FieldInfo
    ): LazyResultPair {
        val result = try {
            row.getLazy(fieldInfo.dbFieldName)
        } catch (e: Exception) {
            throw RepositoryException("error getting value for field ${fieldInfo.dbFieldName}", e)
        }
        return LazyResultPair(
            fieldInfo, try {
                result
            } catch (e: Exception) {
                throw RepositoryException("error resolving value for field ${fieldInfo.dbFieldName}", e)
            }
        )
    }
}
