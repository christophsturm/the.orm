package io.the.orm.mapper

import io.the.orm.dbio.ConnectionProvider
import io.the.orm.dbio.DBResult
import io.the.orm.relations.Relation
import kotlin.reflect.KProperty1
import kotlinx.coroutines.flow.Flow

internal class DefaultResultMapper<Entity : Any>(
    private val resultResolver: ResultResolver<Entity>,
    private val entityCreator: EntityCreator<Entity>
) : ResultMapper<Entity>, SimpleResultMapper<Entity> {

    override suspend fun mapQueryResult(
        queryResult: DBResult,
        fetchRelations: Set<KProperty1<*, Relation>>,
        connectionProvider: ConnectionProvider
    ): Flow<Entity> {
        return mapQueryResult(queryResult)
    }

    override suspend fun mapQueryResult(queryResult: DBResult): Flow<Entity> {
        val parameters: Flow<ResultLine> = resultResolver.getResolvedValues(queryResult)
        return entityCreator.toEntities(parameters, listOf(), listOf())
    }
}
