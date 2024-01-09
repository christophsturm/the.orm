package io.the.orm.internal.classinfo

import failgood.Test
import failgood.tests
import io.the.orm.PKType
import kotlin.test.assertNotNull

@Test
object MappingTest {
    val tests = tests {
        describe("belongs to") {
            it("works eager") {
                data class E(val id: PKType? = null)
                data class BelongsToE(val e: E, val id: PKType? = null)

                val classInfo = ClassInfo(BelongsToE::class, setOf(E::class))
                val rel = assertNotNull(classInfo.entityInfo.belongsToRelations.singleOrNull())
                assert(rel.valueForDb(classInfo.entityWrapper(BelongsToE(E(id = 42)))) == 42L)
            }
        }
    }
}
