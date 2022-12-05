package io.the.orm.internal

import io.the.orm.dbio.ConnectionProvider
import io.the.orm.exp.relations.HasMany
import io.the.orm.internal.classinfo.ClassInfo

internal class HasManyInserter<Entity : Any>(
    private val rootSimpleInserter: Inserter<Entity>,
    private val classInfo: ClassInfo<Entity>,
    private val belongingsInserters: List<Inserter<*>>
) : Inserter<Entity> {
    override suspend fun create(connectionProvider: ConnectionProvider, instance: Entity): Entity {
        rootSimpleInserter.create(connectionProvider, instance)
        classInfo.hasManyRelations.forEachIndexed { index, remoteFieldInfo ->
            @Suppress("UNCHECKED_CAST")
            val inserter = belongingsInserters[index] as Inserter<Any>
            val hasMany = remoteFieldInfo.property.call(instance) as HasMany<*>
//            val classInfo = ClassInfo(remoteFieldInfo.relatedClass!!)
            hasMany.forEach { e ->
                inserter.create(connectionProvider, e)
            }
        }
        return instance
    }
}
