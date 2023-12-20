package io.the.orm.relations

import io.the.orm.PKType
import io.the.orm.RepositoryException

interface Relation

sealed interface BelongsTo<Entity : Any> : Relation {
    fun get(): Entity

    class BelongsToImpl<Entity : Any>(val entity: Entity) : BelongsTo<Entity> {
        override fun get(): Entity {
            return entity
        }
    }

    class BelongsToNotLoaded<Entity : Any>(val pk: PKType) : BelongsTo<Entity> {
        override fun get(): Entity {
            throw RelationNotLoadedException()
        }
    }

    data class Auto<Entity : Any>(var id: PKType? = null) : BelongsTo<Entity> {
        override fun get(): Entity {
            throw RelationNotLoadedException()
        }
    }
}

class RelationNotLoadedException : RepositoryException("not loaded")

fun <Entity : Any> belongsTo() = BelongsTo.Auto<Entity>()

fun <Entity : Any> belongsTo(entity: Entity) = BelongsTo.BelongsToImpl(entity)
