package r2dbcfun.internal

import failfast.describe

object ClassInfoTest {
    val context = describe(ClassInfo::class) {
        it("collects class info for entities") {
            data class Entity(val name: String, val id: Long? = null)

            ClassInfo(Entity::class, IDHandler(Entity::class))
        }

    }
}
