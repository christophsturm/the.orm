package io.the.orm.mapper

import io.the.orm.dbio.DBResult
import kotlinx.coroutines.flow.Flow

interface ResultMapper<T : Any> {
    suspend fun mapQueryResult(queryResult: DBResult): Flow<T>
}
