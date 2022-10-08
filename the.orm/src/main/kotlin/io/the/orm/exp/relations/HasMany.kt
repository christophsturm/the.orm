@file:Suppress("unused", "UNUSED_PARAMETER")

package io.the.orm.exp.relations

import io.the.orm.dbio.ConnectionProvider

/*
Relations support, not yet finished
 */

interface HasMany<Entity : Any> : Set<Entity> {
    // the mutability functions all need a connection provider because they write to the db
    fun add(connectionProvider: ConnectionProvider, entity: Entity)
    fun remove(connectionProvider: ConnectionProvider, entity: Entity)
    fun replace(connectionProvider: ConnectionProvider, newSet: Set<Entity>)
}

/*
 we will also need an ordered HasMany that implements a list.
 */
interface HasManyList<Entity : Any> : List<Entity>
