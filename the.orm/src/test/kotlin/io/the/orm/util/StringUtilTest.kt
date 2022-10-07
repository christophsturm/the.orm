package io.the.orm.util

import failgood.Test
import failgood.describe
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Test
class StringUtilTest {
    val context = describe("string methods") {
        it("can convert camel case to snake case") {
            expectThat("CamelCase".toSnakeCase()).isEqualTo("camel_case")
        }
        it("can convert question mark syntax to indexed syntax") {
            expectThat("values (?,?,?)".toIndexedPlaceholders())
                .isEqualTo("values ($1,$2,$3)")
        }
    }
}
