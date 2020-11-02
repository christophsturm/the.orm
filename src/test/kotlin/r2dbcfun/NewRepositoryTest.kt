package r2dbcfun

import io.kotest.core.spec.style.FunSpec
import strikt.api.expectCatching
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isFailure
import strikt.assertions.isNotNull
import strikt.assertions.message

class NewRepositoryTest : FunSpec({
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

        }
    }

})
