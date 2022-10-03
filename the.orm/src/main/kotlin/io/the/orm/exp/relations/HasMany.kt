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
    companion object {
        operator fun <Entity : Any> invoke(): HasMany<Entity> {
            return Impl()
        }

        class Impl<Entity : Any> : HasMany<Entity> {
            override fun add(connectionProvider: ConnectionProvider, entity: Entity) {
                TODO("Not yet implemented")
            }

            override fun remove(connectionProvider: ConnectionProvider, entity: Entity) {
                TODO("Not yet implemented")
            }

            override fun replace(connectionProvider: ConnectionProvider, newSet: Set<Entity>) {
                TODO("Not yet implemented")
            }

            override val size: Int
                get() = TODO("Not yet implemented")

            override fun isEmpty(): Boolean {
                TODO("Not yet implemented")
            }

            override fun iterator(): Iterator<Entity> {
                TODO("Not yet implemented")
            }

            override fun containsAll(elements: Collection<Entity>): Boolean {
                TODO("Not yet implemented")
            }

            override fun contains(element: Entity): Boolean {
                TODO("Not yet implemented")
            }
        }
    }
}

/*
 we will also need an ordered HasMany that implements a list.
 */
interface HasManyList<Entity : Any> : List<Entity>
