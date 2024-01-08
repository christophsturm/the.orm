package io.the.orm.mapper

import io.the.orm.OrmException
import io.the.orm.dbio.DBResult
import io.the.orm.dbio.DBRow
import io.the.orm.dbio.LazyResult
import io.the.orm.internal.classinfo.ClassInfo
import io.the.orm.internal.classinfo.EntityInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class ResultResolver(private val entityInfo: EntityInfo) {
    private data class LazyResultLine(
        val fields: List<LazyResult<*>>,
        val relations: List<LazyResult<*>>
    )

    suspend fun getResolvedValues(queryResult: DBResult): Flow<ResultLine> {
        val map =
            queryResult.map { row ->
                LazyResultLine(
                    entityInfo.simpleFields.map { fieldInfo -> lazyResult(row, fieldInfo) },
                    entityInfo.belongsToRelations.map { fieldInfo -> lazyResult(row, fieldInfo) }
                )
            }

        return map.map { resultLine ->
            ResultLine(
                resultLine.fields.map { it.resolve() },
                resultLine.relations.map { it.resolve() }
            )
        }
    }

    private fun lazyResult(row: DBRow, fieldInfo: ClassInfo.LocalFieldInfo): LazyResult<*> {
        return try {
            row.getLazy(fieldInfo.dbFieldName)
        } catch (e: Exception) {
            throw OrmException("error getting value for field ${fieldInfo.dbFieldName}", e)
        }
    }
}
