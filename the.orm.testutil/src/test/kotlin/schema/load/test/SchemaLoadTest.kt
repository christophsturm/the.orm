package schema.load.test

import failgood.Test
import failgood.describe
import io.the.orm.test.DBS
import io.the.orm.test.withDb

@Test
class SchemaLoadTest {
    val tests = describe("schema loading") {
        withDb(DBS.h2) {
            it("loads schema.sql from the classpath") {
            }
        }
    }
}
