package io.the.orm.mapper

import io.the.orm.dbio.ConnectionProvider
import io.the.orm.dbio.DBResult
import io.the.orm.relations.Relation
import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KProperty1

interface ResultMapper<T : Any> {
    suspend fun mapQueryResult(
        queryResult: DBResult,
        fetchRelations: Set<KProperty1<*, Relation>> = setOf(),
        connectionProvider: ConnectionProvider
    ): Flow<T>
}
