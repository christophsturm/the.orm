package r2dbcfun.internal

import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import r2dbcfun.PK
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class IDHandlerTest : JUnit5Minutests {

    @Suppress("unused")
    fun tests() = rootContext<Unit> {

        context("assigning an id to a newly created instance") {
            test("assigns a long id") {
                data class ClassWithLongId(val id: Long?, val otherProp: String)

                val instance = ClassWithLongId(null, "string")
                expectThat(IDHandler(ClassWithLongId::class).assignId(instance, 1)).isEqualTo(
                    instance.copy(id = 1)
                )
            }
            test("assigns a PK class instance") {
                data class PKClass(override val id: Long) : PK
                data class ClassWithPKClassId(val id: PKClass?, val otherProp: String)

                val instance = ClassWithPKClassId(null, "string")
                expectThat(IDHandler(ClassWithPKClassId::class).assignId(instance, 1)).isEqualTo(
                    instance.copy(id = PKClass(1))
                )
            }
        }
    }
}
