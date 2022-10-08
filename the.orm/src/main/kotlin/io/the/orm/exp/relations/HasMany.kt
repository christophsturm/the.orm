@file:Suppress("unused", "UNUSED_PARAMETER")

package io.the.orm.exp.relations

/*
Relations support, not yet finished
 */

interface HasMany<Entity : Any> : Set<Entity>

/*
 we will also need an ordered HasMany that implements a list.
 */
interface HasManyList<Entity : Any> : List<Entity>
