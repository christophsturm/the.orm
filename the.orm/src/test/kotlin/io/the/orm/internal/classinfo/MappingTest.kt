package io.the.orm.internal.classinfo

import failgood.Test
import failgood.describe
import io.the.orm.PK
import kotlin.test.assertNotNull

@Test
object MappingTest {
    val tests = describe("mapping") {
        describe("belongs to") {
            it("works eager") {
                data class E(val id: PK? = null)
                data class BelongsToE(val e: E, val id: PK? = null)

                val rel = assertNotNull(ClassInfo(BelongsToE::class, setOf(E::class)).belongsToRelations.singleOrNull())
                assert(rel.valueForDb(BelongsToE(E(id = 42))) == 42L)
            }
        }
    }
}
