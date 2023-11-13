package io.the.orm.internal

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
    override suspend fun create(connectionProvider: ConnectionProvider, instance: Entity): Entity {
        val insertedRoot = rootSimpleInserter.create(connectionProvider, instance)
        val id = classInfo.idHandler!!.readId(insertedRoot)
        classInfo.hasManyRelations.forEachIndexed { index, remoteFieldInfo ->
            @Suppress("UNCHECKED_CAST")
            val repo = belongingsRepos[index] as Repo<Any>
            val hasMany = remoteFieldInfo.property.call(instance) as HasMany<*>
            val fieldInfo = belongingsFieldInfo[index]
            hasMany.forEach { e ->
                val belongsToField = fieldInfo.property.call(e) as? BelongsTo.Auto<*>
                belongsToField?.id = id
                repo.create(connectionProvider, e)
            }
        }
        return insertedRoot
    }
}
