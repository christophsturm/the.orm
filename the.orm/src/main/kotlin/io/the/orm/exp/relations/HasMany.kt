@file:Suppress("unused", "UNUSED_PARAMETER")

package io.the.orm.exp.relations

/*
Relations support, not yet finished
 */

interface HasMany<Entity : Any> : Set<Entity>
fun <T : Any> hasMany(list: Set<T>): HasMany<T> = NewHasMany(list)
fun <T : Any> hasMany(vararg entities: T): HasMany<T> = NewHasMany(entities.asList().toSet())

class NewHasMany<T : Any>(private val list: Set<T>) : HasMany<T>, Set<T> by list {
    override fun equals(other: Any?): Boolean {
        return other is NewHasMany<*>
    }
}

/*
 we will also need an ordered HasMany that implements a list.
 */
interface HasManyList<Entity : Any> : List<Entity>
