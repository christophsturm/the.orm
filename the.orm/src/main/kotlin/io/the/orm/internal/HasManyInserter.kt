package io.the.orm.internal

import io.the.orm.PKType
import io.the.orm.Repo
import io.the.orm.dbio.ConnectionProvider
import io.the.orm.internal.classinfo.ClassInfo
import io.the.orm.internal.classinfo.Field
import io.the.orm.relations.BelongsTo
import io.the.orm.relations.HasMany

internal class HasManyInserter<Entity : Any>(
    private val rootSimpleInserter: Inserter<Entity>,
    private val hasManyFieldInfos: List<ClassInfo.HasManyFieldInfo>,
    private val idField: Field
) : Inserter<Entity> {

    override suspend fun create(
        connectionProvider: ConnectionProvider,
        instance: EntityWrapper<Entity>
    ): EntityWrapper<Entity> {
        // insert the root entity
        val insertedRoot = rootSimpleInserter.create(connectionProvider, instance)
        // and get the id
        val id = idField.property.call(insertedRoot.entity) as PKType
        hasManyFieldInfos.forEach { hasManyFieldInfo ->
            @Suppress("UNCHECKED_CAST") val repo = hasManyFieldInfo.repo as Repo<Any>
            val hasMany = hasManyFieldInfo.field.property.call(instance.entity) as HasMany<*>
            val fieldInfo = hasManyFieldInfo.remoteFieldInfo
            hasMany.forEach { e ->
                val belongsToField =
                    fieldInfo.field.property.call(e) as? BelongsTo.AutoGetFromHasMany<*>
                belongsToField?.id = id
                repo.create(connectionProvider, e)
            }
        }
        return insertedRoot
    }
}
