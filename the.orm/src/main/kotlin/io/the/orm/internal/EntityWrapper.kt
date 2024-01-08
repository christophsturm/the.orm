package io.the.orm.internal

interface EntityWrapper {
    companion object {
        fun <Entity : Any> fromClass(entity: Entity) = EntityWrapperImpl(entity)
    }

    val entity: Any

    fun withId(id: Long): EntityWrapper
}

interface GenericEntityWrapper<Entity : Any> : EntityWrapper {
    override fun withId(id: Long): GenericEntityWrapper<Entity>
}

data class EntityWrapperImpl<Entity : Any>(override val entity: Entity) :
    GenericEntityWrapper<Entity> {
    private val idHandler = IDHandler(entity::class)

    override fun withId(id: Long): GenericEntityWrapper<Entity> {
        return EntityWrapperImpl(idHandler.copyWithId(entity, id))
    }
}
