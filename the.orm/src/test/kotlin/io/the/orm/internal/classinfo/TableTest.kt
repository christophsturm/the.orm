package io.the.orm.internal.classinfo

import failgood.Test
import failgood.tests

@Test
object TableTest {
    val tests = tests {
        it("gets the default table name from the class") {
            assert(Table(ClassInfoTest.Eager.User::class).baseName == "user")
            assert(Table(ClassInfoTest.Eager.User::class).name == "users")
        }
    }
}
