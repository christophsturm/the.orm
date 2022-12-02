@file:Suppress("unused", "UNUSED_PARAMETER")

package io.the.orm.exp.relations

/*
Relations support, not yet finished
 */

class HasMany<ReferencedEntity : Any, Entity : Any> {
    fun get(host: Entity): Set<ReferencedEntity> = emptySet()
}
