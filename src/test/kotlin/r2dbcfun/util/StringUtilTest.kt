package r2dbcfun.util

import io.kotest.core.spec.style.FunSpec
import r2dbcfun.toIndexedPlaceholders
import r2dbcfun.toSnakeCase
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class StringUtilTest :
    FunSpec(
        {
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
    )
