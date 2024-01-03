package io.the.orm.internal

import io.the.orm.PKType
import io.the.orm.Repo
import io.the.orm.dbio.ConnectionProvider
import io.the.orm.internal.classinfo.ClassInfo
import io.the.orm.relations.BelongsTo
import io.the.orm.relations.HasMany

internal class HasManyInserter<Entity : Any>(
    private val rootSimpleInserter: Inserter<Entity>,
    private val classInfo: ClassInfo<Entity>,
    private val belongingsRepos: List<Repo<*>>,
    private val belongingsFieldInfo: List<ClassInfo.BelongsToFieldInfo>
) : Inserter<Entity> {
    private val field = classInfo.idFieldOrThrow().field

    override suspend fun create(connectionProvider: ConnectionProvider, instance: Entity): Entity {
        val insertedRoot = rootSimpleInserter.create(connectionProvider, instance)
        val id = field.property.call(insertedRoot) as PKType
        classInfo.hasManyRelations.forEachIndexed { index, remoteFieldInfo ->
            @Suppress("UNCHECKED_CAST") val repo = belongingsRepos[index] as Repo<Any>
            val hasMany = remoteFieldInfo.field.property.call(instance) as HasMany<*>
            val fieldInfo = belongingsFieldInfo[index]
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
