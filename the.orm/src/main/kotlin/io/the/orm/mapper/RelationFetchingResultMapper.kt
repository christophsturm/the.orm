package io.the.orm.mapper

import io.the.orm.dbio.ConnectionProvider
import io.the.orm.dbio.DBResult
import kotlinx.coroutines.flow.Flow

internal class RelationFetchingResultMapper<Entity : Any>(
    private val resultResolver: ResultResolver<Entity>,
    private val relationFetchingEntityCreator: RelationFetchingEntityCreator<Entity>
) : ResultMapper<Entity> {

    override suspend fun mapQueryResult(queryResult: DBResult, connectionProvider: ConnectionProvider): Flow<Entity> {
        val parameters: Flow<ResultLine> = resultResolver.getResolvedValues(queryResult)
        return relationFetchingEntityCreator.toEntities(parameters, connectionProvider)
    }
}
