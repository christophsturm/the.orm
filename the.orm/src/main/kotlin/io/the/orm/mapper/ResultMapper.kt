package io.the.orm.mapper

import io.the.orm.dbio.ConnectionProvider
import io.the.orm.dbio.DBResult
import kotlinx.coroutines.flow.Flow

interface ResultMapper<T : Any> {
    suspend fun mapQueryResult(queryResult: DBResult, connectionProvider: ConnectionProvider): Flow<T>
}
