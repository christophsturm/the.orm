package io.the.orm.exp.relations

class BelongsTo<Entity : Any>(val entity: Entity) {
    operator fun invoke(): Entity = entity
}
