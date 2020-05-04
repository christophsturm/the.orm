package r2dbcfun

import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class StringUtilTest : JUnit5Minutests {

    @Suppress("unused")
    fun tests() = rootContext<Unit> {

        test("can convert camel case to snake case") {
            expectThat("CamelCase".toSnakeCase()).isEqualTo("camel_case")
        }
    }
}

