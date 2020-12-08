package r2dbcfun.test.functional

import io.kotest.core.spec.style.FunSpec
import kotlinx.coroutines.reactive.awaitSingle
import r2dbcfun.ConnectedRepository
import r2dbcfun.DataIntegrityViolationException
import r2dbcfun.test.autoClose
import r2dbcfun.test.forAllDatabases
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import java.time.LocalDate

class ConstraintViolationFunctionalTest : FunSpec({
    forAllDatabases(this, "ConstraintViolationFT") { connectionFactory ->
        val repo = ConnectedRepository.create<User>(autoClose(connectionFactory.create().awaitSingle()) { it.close() })

        test("throws DataIntegrityViolationException exception on constraint violation") {
            val user = User(
                name = "chris",
                email = "email",
                birthday = LocalDate.parse("2020-06-20")
            )
            repo.create(user)

            expectThrows<DataIntegrityViolationException> {
                repo.create(user)
            }.get { fieldName }.isEqualTo(User::email)
        }
    }
})
