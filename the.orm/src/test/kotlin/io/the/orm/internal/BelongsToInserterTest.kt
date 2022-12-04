package io.the.orm.internal

import failgood.Test
import failgood.describe
import io.the.orm.dbio.ConnectionProvider

@Test
object BelongsToInserterTest {
    val context = describe<BelongsToInserter<*>> {
    }
}

class BelongsToInserter<Entity : Any> : Inserter<Entity> {
    override suspend fun create(connection: ConnectionProvider, instance: Entity): Entity {
        TODO()
    }
}
