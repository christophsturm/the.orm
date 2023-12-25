package io.the.orm.mapper

import io.the.orm.PKType
import io.the.orm.RepositoryException
import io.the.orm.internal.classinfo.ClassInfo
import io.the.orm.relations.BelongsTo
import io.the.orm.relations.LazyHasMany
import kotlin.reflect.KParameter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal interface EntityCreator<Entity : Any> {
    fun toEntities(
        results: Flow<ResultLine>,
        relations: List<Map<PKType, Any>?> = listOf(),
        hasManyRelations: List<Map<PKType, Set<Entity>>?>?
    ): Flow<Entity>
}

internal class StreamingEntityCreator<Entity : Any>(private val classInfo: ClassInfo<Entity>) :
    EntityCreator<Entity> {
    private val idFieldIndexOrNull: Int? =
        classInfo.simpleFields.indexOfFirst { it.dbFieldName == "id" }.takeIf { it >= 0 }

    override fun toEntities(
        results: Flow<ResultLine>,
        relations: List<Map<PKType, Any>?>,
        hasManyRelations: List<Map<PKType, Set<Entity>>?>?
    ): Flow<Entity> {
        return results
            .map { values ->
                val id = idFieldIndexOrNull?.let { values.fields[it] as PKType }
                // the map that collects values for all constructor parameters of the entity
                val parameterValueCollector =
                    values.fields.withIndex().associateTo(HashMap()) { (index, value) ->
                        val fieldInfo = classInfo.simpleFields[index]
                        val parameterValue = fieldInfo.fieldConverter.dbValueToParameter(value)
                        Pair(fieldInfo.field, parameterValue)
                    }
                values.relations.withIndex().associateTo(parameterValueCollector) { (index, value)
                    ->
                    val fieldInfo = classInfo.belongsToRelations[index]
                    val relationValues = relations[index]
                    if (relationValues != null)
                        Pair(fieldInfo.field, relationValues[value as PKType])
                    else Pair(fieldInfo.field, BelongsTo.BelongsToNotLoaded<Any>(value as PKType))
                }
                classInfo.hasManyRelations.withIndex().associateTo(parameterValueCollector) {
                    (index, it) ->
                    val loadedEntries = hasManyRelations?.get(index)
                    if (loadedEntries != null) Pair(it.field, LazyHasMany<Any>(loadedEntries[id]))
                    else Pair(it.field, LazyHasMany())
                }
            }
            .map { parameterValues ->
                val constructorParameters =
                    parameterValues.mapKeys { (key, value) -> key.constructorParameter }
                try {
                    classInfo.constructor.callBy(constructorParameters)
                } catch (e: Exception) {
                    throw RepositoryException(
                        "error invoking constructor for ${classInfo.name}.\n parameters:${constructorParameters.friendlyString()}",
                        e
                    )
                }
            }
    }
}

fun Map<KParameter, Any?>.friendlyString() =
    entries.joinToString { """${it.key.name}=>${it.value}(${it.key.type})""" }
