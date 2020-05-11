package r2dbcfun.internal

import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class IdAssignerTest : JUnit5Minutests {

    @Suppress("unused")
    fun tests() = rootContext<Unit> {

        test("assigns an id") {
            data class ClassWithLongId(val id: Long?, val otherProp: String)

            val instance = ClassWithLongId(null, "string")
            expectThat(IdAssigner(ClassWithLongId::class).assignId(instance, 1)).isEqualTo(
                instance.copy(id = 1)
            )

        }
    }
}
