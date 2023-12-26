package io.the.orm.relations

import io.the.orm.OrmException
import io.the.orm.PKType

interface Relation

sealed interface BelongsTo<Entity : Any> : Relation {
    fun get(): Entity

    data class BelongsToImpl<Entity : Any>(val entity: Entity) : BelongsTo<Entity> {
        override fun get(): Entity {
            return entity
        }
    }

    data class BelongsToNotLoaded<Entity : Any>(val pk: PKType) : BelongsTo<Entity> {
        override fun get(): Entity {
            throw RelationNotLoadedException()
        }
    }

    /** the id will be set by the opposite side relation */
    data class AutoGetFromHasMany<Entity : Any>(var id: PKType? = null) : BelongsTo<Entity> {
        override fun get(): Entity {
            throw RelationNotLoadedException()
        }
    }
}

class RelationNotLoadedException : OrmException("not loaded")

fun <Entity : Any> belongsTo() = BelongsTo.AutoGetFromHasMany<Entity>()

fun <Entity : Any> belongsTo(entity: Entity) = BelongsTo.BelongsToImpl(entity)
