package r2dbcfun.test.functional

import failfast.describe
import r2dbcfun.ConnectedRepository
import r2dbcfun.UniqueConstraintViolatedException
import r2dbcfun.test.DBS
import r2dbcfun.test.forAllDatabases
import strikt.api.expectThrows
import strikt.assertions.endsWith
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import java.time.LocalDate

object ConstraintViolationFunctionalTest {
    val context = describe("constraint error handling") {
        forAllDatabases(databases = DBS.databases) { createConnectionProvider ->
            val repo = ConnectedRepository.create<User>(createConnectionProvider())

            it("throws DataIntegrityViolationException exception on constraint violation") {
                val user = User(
                    name = "chris",
                    email = "email",
                    birthday = LocalDate.parse("2020-06-20")
                )
                repo.create(user)

                expectThrows<UniqueConstraintViolatedException> {
                    repo.create(user)
                }.and {
                    get { message }.isNotNull().endsWith("field:email value:email")
                    get { field }.isEqualTo(User::email)
                    get { fieldValue }.isEqualTo("email")
                }
            }
        }
    }
}
