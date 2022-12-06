package io.the.orm.internal

import io.the.orm.dbio.ConnectionProvider
import io.the.orm.exp.relations.BelongsTo
import io.the.orm.exp.relations.HasMany
import io.the.orm.internal.classinfo.ClassInfo

internal class HasManyInserter<Entity : Any>(
    private val rootSimpleInserter: Inserter<Entity>,
    private val classInfo: ClassInfo<Entity>,
    private val belongingsInserters: List<Inserter<*>>,
    private val belongingsFieldInfo: List<ClassInfo.LocalFieldInfo>
) : Inserter<Entity> {
    override suspend fun create(connectionProvider: ConnectionProvider, instance: Entity): Entity {
        val insertedRoot = rootSimpleInserter.create(connectionProvider, instance)
        val id = classInfo.idHandler.readId(insertedRoot)
        classInfo.hasManyRelations.forEachIndexed { index, remoteFieldInfo ->
            @Suppress("UNCHECKED_CAST")
            val inserter = belongingsInserters[index] as Inserter<Any>
            val hasMany = remoteFieldInfo.property.call(instance) as HasMany<*>
            val fieldInfo = belongingsFieldInfo[index]
            hasMany.forEach { e ->
                val belongsToField = fieldInfo.property.call(e) as? BelongsTo.Auto<*>
                belongsToField?.id = id
                inserter.create(connectionProvider, e)
            }
        }
        return insertedRoot
    }
}
