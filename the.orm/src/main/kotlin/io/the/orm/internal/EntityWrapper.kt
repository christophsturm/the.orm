package io.the.orm.internal

import io.the.orm.internal.classinfo.Field

interface EntityWrapper {
    companion object {
        internal fun <Entity : Any> fromClass(entity: Entity, idHandler: IDHandler<Entity>) =
            EntityWrapperImpl(entity, idHandler)
    }

    val entity: Any

    fun withId(id: Long): EntityWrapper

    fun get(field: Field): Any?
}

interface GenericEntityWrapper<Entity : Any> : EntityWrapper {
    override fun withId(id: Long): GenericEntityWrapper<Entity>
}

internal data class EntityWrapperImpl<Entity : Any>(
    override val entity: Entity,
    private val idHandler: IDHandler<Entity>
) : GenericEntityWrapper<Entity> {

    override fun withId(id: Long): GenericEntityWrapper<Entity> {
        return EntityWrapperImpl(idHandler.copyWithId(entity, id), idHandler)
    }

    override fun get(field: Field): Any? {
        return field.property.call(entity)
    }
}
