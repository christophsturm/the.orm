package io.the.orm.internal

import failgood.Test
import failgood.describe
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Test
class IDHandlerTest {
    data class ClassWithLongId(val id: Long?, val otherProp: String)
    data class PK(val id: Long)
    data class ClassWithPKClassId(val id: PK?, val otherProp: String)
    val context = describe(IDHandler::class) {
        describe("assigning an id to a newly created instance") {
            it("assigns a Long id") {
                val instance = ClassWithLongId(null, "string")
                expectThat(IDHandler(ClassWithLongId::class).assignId(instance, 1))
                    .isEqualTo(instance.copy(id = 1))
            }
            it("assigns a PK class instance") {
                val instance = ClassWithPKClassId(null, "string")
                expectThat(IDHandler(ClassWithPKClassId::class).assignId(instance, 1))
                    .isEqualTo(instance.copy(id = PK(1)))
            }
        }
        describe("reading the id") {
            it("reads a Long id") {
                val instance = ClassWithLongId(10, "string")
                assert(IDHandler(ClassWithLongId::class).readId(instance) == 10L)
            }
            ignore("reads a PK Long id") {
            }
        }
    }
}
