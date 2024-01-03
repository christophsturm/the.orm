package io.the.orm.internal

import failgood.Test
import failgood.describe
import failgood.tests
import io.the.orm.PKType
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Test
class IDHandlerTest {
    data class ClassWithPkId(val id: PKType?, val otherProp: String)

    val context = tests {
        describe("assigning an id to a newly created instance") {
            it("assigns a Long id") {
                val instance = ClassWithPkId(null, "string")
                expectThat(IDHandler(ClassWithPkId::class).copyWithId(instance, 1))
                    .isEqualTo(instance.copy(id = 1))
            }
        }
        describe("reading the id") {
            it("reads a Long id") {
                val instance = ClassWithPkId(10, "string")
                assert(IDHandler(ClassWithPkId::class).readId(instance) == 10L)
            }
        }
    }
}
