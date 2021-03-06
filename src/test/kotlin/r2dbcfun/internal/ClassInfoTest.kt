package r2dbcfun.internal

import failfast.describe
import strikt.api.expectThat
import strikt.assertions.isEqualTo

object ClassInfoTest {
    val context = describe(ClassInfo::class) {
        data class Entity(val name: String, val id: Long? = null)

        val classInfo = ClassInfo(Entity::class, IDHandler(Entity::class), setOf())
        it("knows the class name") {
            expectThat(classInfo.name).isEqualTo("Entity")
        }

    }
}
