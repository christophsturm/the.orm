package io.the.orm.exp.relations

import io.the.orm.PK
import io.the.orm.RepositoryException
import kotlin.reflect.KClass
interface Relation

sealed interface BelongsTo<Entity : Any> : Relation {
    fun id(): PK
    fun get(): Entity
    class BelongsToImpl<Entity : Any>(val entity: Entity) : BelongsTo<Entity> {
        override fun id(): PK {
            return 0
        }

        override fun get(): Entity {
            return entity
        }
    }
    class BelongsToNotLoaded<Entity : Any>(c: KClass<Entity>, val pk: PK) : BelongsTo<Entity> {
        override fun id(): PK {
            return pk
        }

        override fun get(): Entity {
            throw RelationNotLoadedException()
        }
    }
    data class Auto<Entity : Any>(var id: PK? = null) : BelongsTo<Entity> {
        override fun id(): PK {
            return id ?: throw RepositoryException("id missing")
        }
        override fun get(): Entity {
            throw RelationNotLoadedException()
        }
    }
}

class RelationNotLoadedException : RepositoryException("not loaded")

fun <Entity : Any> belongsTo() = BelongsTo.Auto<Entity>()
fun <Entity : Any> belongsTo(entity: Entity) = BelongsTo.BelongsToImpl(entity)
