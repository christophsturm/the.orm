package io.the.orm.mapper

import io.the.orm.dbio.ConnectionProvider
import io.the.orm.dbio.DBResult
import io.the.orm.relations.Relation
import kotlin.reflect.KProperty1
import kotlinx.coroutines.flow.Flow

internal class RelationFetchingResultMapper<Entity : Any>(
    private val resultResolver: ResultResolver,
    private val relationFetchingEntityCreator: RelationFetchingEntityCreator<Entity>
) : ResultMapper<Entity> {

    override suspend fun mapQueryResult(
        queryResult: DBResult,
        fetchRelations: Set<KProperty1<*, Relation>>,
        connectionProvider: ConnectionProvider
    ): Flow<Entity> {
        val parameters: Flow<ResultLine> = resultResolver.getResolvedValues(queryResult)
        return relationFetchingEntityCreator.toEntities(
            parameters,
            fetchRelations,
            connectionProvider
        )
    }
}
