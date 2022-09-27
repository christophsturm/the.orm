package io.the.orm.exp.relations

import failgood.Test
import failgood.describe

@Test
object BelongsToTest {
    data class Entity(val name: String, val id: Long? = null)

    val tests = describe<BelongsTo<Entity>> {
        val entity = Entity("blah")
        val subject = BelongsTo(entity)
        it("can reference the entity") {
            assert(subject() == entity)
        }
    }
}
