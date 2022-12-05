@file:Suppress("unused", "UNUSED_PARAMETER")

package io.the.orm.exp.relations

/*
Relations support, not yet finished
 */

interface HasMany<Entity : Any> : Set<Entity>
fun <T : Any> hasMany(list: Set<T>): HasMany<T> = NewHasMany(list)
fun <T : Any> hasMany(vararg entities: T): HasMany<T> = NewHasMany(entities.asList().toSet())

class NewHasMany<T : Any>(private val list: Set<T>) : HasMany<T>, Set<T> by list

class HasManyImpl<Entity : Any> : HasMany<Entity> {
    override val size: Int = 0

    override fun contains(element: Entity): Boolean = false

    override fun containsAll(elements: Collection<Entity>): Boolean = false

    override fun isEmpty(): Boolean = true

    override fun iterator(): Iterator<Entity> {
        TODO("Not yet implemented")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HasManyImpl<*>

        if (size != other.size) return false

        return true
    }

    override fun hashCode(): Int {
        return size
    }
}

/*
 we will also need an ordered HasMany that implements a list.
 */
interface HasManyList<Entity : Any> : List<Entity>
