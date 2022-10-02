package io.the.orm.mapper

import io.the.orm.dbio.ConnectionProvider
import io.the.orm.dbio.DBResult
import io.the.orm.dbio.LazyResult
import io.the.orm.internal.classinfo.ClassInfo
import kotlinx.coroutines.flow.Flow

internal class DefaultResultMapper<Entity : Any>(
    private val resultResolver: ResultResolver<Entity>,
    private val entityCreator: EntityCreator<Entity>
) : ResultMapper<Entity> {

    override suspend fun mapQueryResult(queryResult: DBResult, connectionProvider: ConnectionProvider): Flow<Entity> {
        val parameters: Flow<ResultLine> = resultResolver.getResolvedValues(queryResult)
        return entityCreator.toEntities(parameters, connectionProvider)
    }
}
internal data class LazyResultPair(val fieldInfo: ClassInfo.FieldInfo, val lazyResult: LazyResult<Any?>)
internal data class ResultPair(val fieldInfo: ClassInfo.FieldInfo, val valueFromDb: Any?)
internal data class LazyResultLine(val fields: List<LazyResultPair>, val relations: List<LazyResultPair>)
internal data class ResultLine(val fields: List<ResultPair>, val relations: List<ResultPair>)
