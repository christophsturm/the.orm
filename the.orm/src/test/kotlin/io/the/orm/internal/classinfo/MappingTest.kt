package io.the.orm.internal.classinfo

import failgood.Test
import failgood.tests
import io.the.orm.PKType
import io.the.orm.internal.EntityWrapper
import kotlin.test.assertNotNull

@Test
object MappingTest {
    val tests = tests {
        describe("belongs to") {
            it("works eager") {
                data class E(val id: PKType? = null)
                data class BelongsToE(val e: E, val id: PKType? = null)

                val rel =
                    assertNotNull(
                        ClassInfo(BelongsToE::class, setOf(E::class))
                            .belongsToRelations
                            .singleOrNull()
                    )
                assert(rel.valueForDb(EntityWrapper(BelongsToE(E(id = 42)))) == 42L)
            }
        }
    }
}
