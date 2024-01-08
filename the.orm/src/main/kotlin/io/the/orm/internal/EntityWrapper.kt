package io.the.orm.internal

internal data class EntityWrapper<Entity : Any>(val entity: Entity) {
    fun withId(id: Long, idHandler: IDHandler<Entity>): EntityWrapper<Entity> {
        return EntityWrapper(idHandler.copyWithId(entity, id))
    }
}
