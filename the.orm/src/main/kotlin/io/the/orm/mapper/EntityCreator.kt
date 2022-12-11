package io.the.orm.mapper

import io.the.orm.PK
import io.the.orm.RepositoryException
import io.the.orm.exp.relations.BelongsTo
import io.the.orm.exp.relations.LazyHasMany
import io.the.orm.internal.classinfo.ClassInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.reflect.KParameter

internal interface EntityCreator<Entity : Any> {
    fun toEntities(
        results: Flow<ResultLine>,
        relations: List<Map<PK, Any>?> = listOf(),
        hasManyRelations: List<Map<PK, Set<Entity>>?>?
    ): Flow<Entity>
}

internal class StreamingEntityCreator<Entity : Any>(private val classInfo: ClassInfo<Entity>) : EntityCreator<Entity> {
    private val idFieldIndex = classInfo.simpleFieldInfo.indexOfFirst { it.dbFieldName == "id" }

    override fun toEntities(
        results: Flow<ResultLine>,
        relations: List<Map<PK, Any>?>,
        hasManyRelations: List<Map<PK, Set<Entity>>?>?
    ): Flow<Entity> {
        return results.map { values ->
            val id = values.fields[idFieldIndex] as PK
            val map = values.fields.withIndex().associateTo(HashMap()) { (index, value) ->
                val fieldInfo = classInfo.simpleFieldInfo[index]
                val parameterValue = fieldInfo.fieldConverter.dbValueToParameter(value)
                Pair(fieldInfo.constructorParameter, parameterValue)
            }
            values.relations.withIndex().associateTo(map) { (index, value) ->
                val fieldInfo = classInfo.belongsToRelations[index]
                val relationValues = relations[index]
                if (relationValues != null)
                    Pair(fieldInfo.constructorParameter, relationValues[value as PK])
                else
                    Pair(
                        fieldInfo.constructorParameter,
                        BelongsTo.BelongsToNotLoaded(fieldInfo.relatedClass, value as PK)
                    )
            }
            classInfo.hasManyRelations.withIndex().associateTo(map) { (index, it) ->
                val loadedEntries = hasManyRelations?.get(index)
                if (loadedEntries != null)
                    Pair(it.constructorParameter, LazyHasMany<Any>(loadedEntries[id]))

                Pair(it.constructorParameter, LazyHasMany<Any>())
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

fun Map<KParameter, Any?>.friendlyString() =
    entries.joinToString { """${it.key.name}=>${it.value}(${it.key.type})""" }
