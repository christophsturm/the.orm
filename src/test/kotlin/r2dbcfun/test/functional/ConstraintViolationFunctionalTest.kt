package r2dbcfun.test.functional

import failfast.context
import kotlinx.coroutines.reactive.awaitSingle
import r2dbcfun.ConnectedRepository
import r2dbcfun.DataIntegrityViolationException
import r2dbcfun.test.forAllDatabases
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import java.time.LocalDate

object ConstraintViolationFunctionalTest {
    val context = context("constraint error handling") {
        forAllDatabases { connectionFactory ->
            val repo =
                ConnectedRepository.create<User>(autoClose(connectionFactory.create().awaitSingle()) { it.close() })

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
    }
}
