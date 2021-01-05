package r2dbcfun

import failfast.context
import r2dbcfun.query.QueryFactory
import r2dbcfun.test.TestObjects.Entity
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isFailure
import strikt.assertions.isNotNull
import strikt.assertions.message

object RepositoryTest {
    val context = context {
        test("returns a query factory") {
            val queryFactory = Repository.create<Entity>().queryFactory
            expectThat(queryFactory).isA<QueryFactory<Entity>>()
        }
        context("fail fast error handling") {
            test("fails fast if PK has more than one field") {
                data class MismatchPK(override val id: Long, val blah: String) : PK
                data class Mismatch(val id: MismatchPK)
                expectCatching { Repository.create<Mismatch>() }.isFailure()
                    .isA<RepositoryException>()
                    .message
                    .isNotNull()
                    .contains("PK classes must have a single field of type long")
            }
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
