package io.the.orm.internal

import failgood.Test
import failgood.describe
import io.the.orm.PK
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Test
class IDHandlerTest {
    data class ClassWithPkId(val id: PK?, val otherProp: String)
    data class PKClass(val id: PK)
    data class ClassWithPKClassId(val id: PKClass?, val otherProp: String)
    val context = describe(IDHandler::class) {
        describe("assigning an id to a newly created instance") {
            it("assigns a Long id") {
                val instance = ClassWithPkId(null, "string")
                expectThat(IDHandler(ClassWithPkId::class).assignId(instance, 1))
                    .isEqualTo(instance.copy(id = 1))
            }
            it("assigns a PK class instance") {
                val instance = ClassWithPKClassId(null, "string")
                expectThat(IDHandler(ClassWithPKClassId::class).assignId(instance, 1))
                    .isEqualTo(instance.copy(id = PKClass(1)))
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
