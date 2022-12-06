@file:Suppress("unused", "UNUSED_PARAMETER")

package io.the.orm.exp.relations

import io.the.orm.Repo
import io.the.orm.RepositoryException

/*
Relations support, not yet finished
 */

interface HasMany<Entity : Any> : Set<Entity>

fun <T : Any> hasMany(list: Set<T>): HasMany<T> = NewHasMany(list)
fun <T : Any> hasMany(vararg entities: T): HasMany<T> = NewHasMany(entities.asList().toSet())

class NewHasMany<T : Any>(internal val list: Set<T>) : HasMany<T>, Set<T> by list {
    override fun equals(other: Any?): Boolean {
        return other is NewHasMany<*> && other.list == list
    }
}

internal class LazyHasMany<T : Any> : HasMany<T> {
    internal var backingSet: Set<T>? = null
    override val size: Int
        get() {
            throwIfUnfetched()
            return backingSet!!.size
        }

    private fun throwIfUnfetched() {
        if (backingSet == null)
            throw RepositoryException("Has Many Relation is not fetched")
    }

    override fun contains(element: T): Boolean {
        throwIfUnfetched()
        return backingSet!!.contains(element)
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        throwIfUnfetched()
        return backingSet!!.containsAll(elements)
    }

    override fun isEmpty(): Boolean {
        throwIfUnfetched()
        return backingSet!!.isEmpty()
    }

    override fun iterator(): Iterator<T> {
        throwIfUnfetched()
        return backingSet!!.iterator()
    }
}

/*
 we will also need an ordered HasMany that implements a list.
 */
interface HasManyList<Entity : Any> : List<Entity>
