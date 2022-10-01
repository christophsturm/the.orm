package io.the.orm.mapper

import io.the.orm.RepositoryException
import io.the.orm.internal.classinfo.ClassInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class EntityCreator<Entity : Any>(private val classInfo: ClassInfo<Entity>) {
    fun toEntities(parameters: Flow<List<ResultPair>>): Flow<Entity> {
        return parameters.map { values ->
            values.associateTo(HashMap()) { (fieldInfo, result) ->
                val value = fieldInfo.fieldConverter.dbValueToParameter(result)
                Pair(fieldInfo.constructorParameter, value)
            }
        }.map {
            try {
                classInfo.constructor.callBy(it)
            } catch (e: IllegalArgumentException) {
                throw RepositoryException(
                    "error invoking constructor for ${classInfo.name}. parameters:$it", e
                )
            }
        }
    }
}
