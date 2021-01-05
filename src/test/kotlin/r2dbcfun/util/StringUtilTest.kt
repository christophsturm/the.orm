package r2dbcfun.util

import failfast.context
import strikt.api.expectThat
import strikt.assertions.isEqualTo

object StringUtilTest {
    val context =
        context("string methods") {
            test("can convert camel case to snake case") {
                expectThat("CamelCase".toSnakeCase()).isEqualTo("camel_case")
            }
            test("can convert question mark syntax to indexed syntax") {
                expectThat("values (?,?,?)".toIndexedPlaceholders())
                    .isEqualTo("values ($1,$2,$3)")
            }
        }
}
