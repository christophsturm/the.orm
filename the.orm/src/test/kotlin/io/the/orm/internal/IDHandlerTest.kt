package io.the.orm.internal

import failgood.Test
import failgood.describe
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Test
class IDHandlerTest {
    val context = describe(IDHandler::class) {
        context("assigning an id to a newly created instance") {
            test("assigns a long id") {
                data class ClassWithLongId(val id: Long?, val otherProp: String)

                val instance = ClassWithLongId(null, "string")
                expectThat(IDHandler(ClassWithLongId::class).assignId(instance, 1))
                    .isEqualTo(instance.copy(id = 1))
            }
            test("assigns a PK class instance") {
                data class PK(val id: Long)
                data class ClassWithPKClassId(val id: PK?, val otherProp: String)

                val instance = ClassWithPKClassId(null, "string")
                expectThat(IDHandler(ClassWithPKClassId::class).assignId(instance, 1))
                    .isEqualTo(instance.copy(id = PK(1)))
            }
        }
    }
}
