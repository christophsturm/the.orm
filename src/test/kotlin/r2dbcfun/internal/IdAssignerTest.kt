package r2dbcfun.internal

import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class IdAssignerTest : JUnit5Minutests {

    @Suppress("unused")
    fun tests() = rootContext<Unit> {

        context("returning an instance with id for an instance without id") {
            test("assigns a long id") {
                data class ClassWithLongId(val id: Long?, val otherProp: String)

                val instance = ClassWithLongId(null, "string")
                expectThat(IdAssigner(ClassWithLongId::class).assignId(instance, 1)).isEqualTo(
                    instance.copy(id = 1)
                )
            }
            test("assigns a PK class instance") {
                data class PK(val id: Long)
                data class ClassWithLongId(val id: PK?, val otherProp: String)

                val instance = ClassWithLongId(null, "string")
                expectThat(IdAssigner(ClassWithLongId::class).assignId(instance, 1)).isEqualTo(
                    instance.copy(id = PK(1))
                )
            }
        }
    }
}
