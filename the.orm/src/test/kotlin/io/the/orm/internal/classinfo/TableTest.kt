package io.the.orm.internal.classinfo

import failgood.Test
import failgood.describe

@Test
object TableTest {
    val tests = describe {
        it("gets the default table name from the class") {
            assert(Table(Eager.User::class).baseName == "user")
            assert(Table(Eager.User::class).name == "users")
        }
    }
}
