package io.the.orm.mapper

import io.the.orm.RepositoryException
import io.the.orm.dbio.DBResult
import io.the.orm.dbio.DBRow
import io.the.orm.dbio.LazyResult
import io.the.orm.internal.classinfo.ClassInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class ResultResolver<Entity : Any>(private val classInfo: ClassInfo<Entity>) {
    suspend fun getResolvedValues(queryResult: DBResult): Flow<ResultLine> {
        val map = queryResult.map { row ->
            LazyResultLine(classInfo.fields.map { fieldInfo ->
                lazyResult(row, fieldInfo)
            },
                classInfo.relations.map { fieldInfo ->
                    lazyResult(row, fieldInfo)
                }
            )
        }

        return map.map {
            ResultLine(
                it.fields.map { it.resolve() },
                it.relations.map { it.resolve() })
        }
    }

    private fun lazyResult(
        row: DBRow,
        fieldInfo: ClassInfo.LocalFieldInfo
    ): LazyResult<*> {
        return try {
            row.getLazy(fieldInfo.dbFieldName)
        } catch (e: Exception) {
            throw RepositoryException("error getting value for field ${fieldInfo.dbFieldName}", e)
        }
    }
}