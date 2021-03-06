package r2dbcfun.internal

import failfast.describe

object ClassInfoTest {
    val context = describe(ClassCreator::class) {
        it("collects class info for entities") {
            data class Entity(val name: String, val id: Long? = null)

            ClassCreator(Entity::class, IDHandler(Entity::class), setOf())
        }

    }
}
