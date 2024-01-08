package io.the.orm.mapper

import io.the.orm.dbio.DBResult
import io.the.orm.internal.classinfo.ClassInfo
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

interface SimpleResultMapper<Entity : Any> {
    companion object {
        fun <T : Any> forClass(entity: KClass<T>): SimpleResultMapper<T> {
            val classInfo = ClassInfo(entity)
            return DefaultResultMapper(
                ResultResolver(classInfo.entityInfo),
                StreamingEntityCreator(classInfo)
            )
        }
    }

    suspend fun mapQueryResult(queryResult: DBResult): Flow<Entity>
}
