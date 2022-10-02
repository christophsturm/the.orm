package io.the.orm.mapper

import io.the.orm.RepositoryException
import io.the.orm.dbio.ConnectionProvider
import io.the.orm.internal.classinfo.ClassInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.reflect.KParameter

internal interface EntityCreator<Entity : Any> {
    fun toEntities(results: Flow<List<ResultPair>>, connectionProvider: ConnectionProvider): Flow<Entity>
}

internal class StreamingEntityCreator<Entity : Any>(private val classInfo: ClassInfo<Entity>) : EntityCreator<Entity> {
    override fun toEntities(results: Flow<List<ResultPair>>, connectionProvider: ConnectionProvider): Flow<Entity> {
        return results.map { values ->
            values.associateTo(HashMap()) { (fieldInfo, result) ->
                val value = fieldInfo.fieldConverter.dbValueToParameter(result)
                Pair(fieldInfo.constructorParameter, value)
            }
        }.map {
            try {
                classInfo.constructor.callBy(it)
            } catch (e: Exception) {
                throw RepositoryException(
                    "error invoking constructor for ${classInfo.name}.\n parameters:${it.friendlyString()}", e
                )
            }
        }
    }
}
private fun Map<KParameter, Any?>.friendlyString() =
    entries.joinToString { it.key.name + "=>" + it.value }
