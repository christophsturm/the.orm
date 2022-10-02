package io.the.orm.mapper

import io.the.orm.PK
import io.the.orm.RepositoryException
import io.the.orm.internal.classinfo.ClassInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.reflect.KParameter

internal interface EntityCreator<Entity : Any> {
    fun toEntities(results: Flow<ResultLine>, relations: List<Map<PK, Any>> = listOf()): Flow<Entity>
}

internal class StreamingEntityCreator<Entity : Any>(private val classInfo: ClassInfo<Entity>) : EntityCreator<Entity> {
    override fun toEntities(results: Flow<ResultLine>, relations: List<Map<PK, Any>>): Flow<Entity> {
        return results.map { values ->
            val v = values.fields // no need to handle relations here because this is only used if we have none
            val map = v.withIndex().associateTo(HashMap()) { (index, value) ->
                val fieldInfo = classInfo.fields[index]
                val v = fieldInfo.fieldConverter.dbValueToParameter(value)
                Pair(fieldInfo.constructorParameter, v)
            }
            values.relations.withIndex().associateTo(map) { (index, value) ->
                val fieldInfo = classInfo.relations[index]
                Pair(fieldInfo.constructorParameter, relations[index].get(value as PK))
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
