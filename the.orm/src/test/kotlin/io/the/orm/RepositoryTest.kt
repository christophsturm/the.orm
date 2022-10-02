package io.the.orm

import failgood.Test
import failgood.describe
import io.the.orm.query.QueryFactory
import io.the.orm.test.TestObjects.Entity
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isFailure
import strikt.assertions.isNotNull
import strikt.assertions.message

@Test
object RepositoryTest {
    val context = describe(Repository::class) {
        test("returns a query factory") {
            val queryFactory = Repository.create<Entity>().queryFactory
            expectThat(queryFactory).isA<QueryFactory<Entity>>()
        }
        context("fail fast error handling") {
            test("fails if class contains unsupported fields") {
                data class Unsupported(val field: String)
                data class ClassWithUnsupportedType(val id: Long, val unsupported: Unsupported)
                expectCatching { Repository.create<ClassWithUnsupportedType>() }.isFailure()
                    .isA<RepositoryException>()
                    .message
                    .isNotNull()
                    .contains("type Unsupported not supported")
            }
            test("fails if class has no id field") {
                expectCatching { Repository.create<Any>() }.isFailure()
                    .isA<RepositoryException>()
                    .message
                    .isNotNull()
                    .contains("class Any has no field named id")
            }
        }
    }
}
