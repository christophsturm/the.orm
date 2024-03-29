package io.the.orm

import failgood.Test
import failgood.tests
import io.the.orm.query.QueryFactory
import io.the.orm.test.TestObjects.Entity
import kotlin.test.assertNotNull
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isFailure
import strikt.assertions.isNotNull
import strikt.assertions.message

@Test
object RepositoryTest {
    val context = tests {
        test("returns a query factory") {
            val queryFactory = Repo.create<Entity>().queryFactory
            expectThat(queryFactory).isA<QueryFactory<Entity>>()
        }
        context("fail fast error handling") {
            test("fails if class contains unsupported fields") {
                data class Unsupported(val field: String)
                data class ClassWithUnsupportedType(val id: Long, val unsupported: Unsupported)
                expectCatching { Repo.create<ClassWithUnsupportedType>() }
                    .isFailure()
                    .isA<OrmException>()
                    .message
                    .isNotNull()
                    .contains("type Unsupported not supported")
            }
            test("fails if class has no id field") {
                data class WithoutId(val name: String)

                val result = runCatching { Repo.create<WithoutId>() }
                val exception = assertNotNull(result.exceptionOrNull())
                assert(
                    exception is OrmException &&
                        (exception.message?.contains("class WithoutId has no field named id") ==
                            true)
                ) {
                    exception.stackTraceToString()
                }
            }
        }
    }
}
