package io.the.orm.mapper

import io.the.orm.dbio.DBResult
import io.the.orm.dbio.LazyResult
import io.the.orm.internal.classinfo.ClassInfo
import kotlinx.coroutines.flow.Flow

internal class DefaultResultMapper<Entity : Any>(
    private val resultResolver: ResultResolver<Entity>,
    private val entityCreator: EntityCreator<Entity>
) : ResultMapper<Entity> {

    override suspend fun mapQueryResult(queryResult: DBResult): Flow<Entity> {
        val parameters: Flow<List<ResultPair>> = resultResolver.getResolvedValues(queryResult)
        return entityCreator.toEntities(parameters)
    }
}
internal data class LazyResultPair(val fieldInfo: ClassInfo.FieldInfo, val lazyResult: LazyResult<Any?>)
internal data class ResultPair(val fieldInfo: ClassInfo.FieldInfo, val valueFromDb: Any?)
