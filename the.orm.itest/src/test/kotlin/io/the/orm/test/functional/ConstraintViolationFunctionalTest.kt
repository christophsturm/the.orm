package io.the.orm.test.functional

import failgood.Test
import io.the.orm.test.DBS
import io.the.orm.test.describeOnAllDbs
import strikt.api.expectThrows
import strikt.assertions.endsWith
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import java.time.LocalDate

@Test
class ConstraintViolationFunctionalTest {
    val context = describeOnAllDbs("constraint error handling", DBS.databases) { createConnectionProvider ->

        val repo by dependency({ io.the.orm.ConnectedRepository.create<User>(createConnectionProvider()) })

        it("throws DataIntegrityViolationException exception on constraint violation") {
            val user = User(
                name = "chris",
                email = "email",
                birthday = LocalDate.parse("2020-06-20")
            )
            repo.create(user)

            expectThrows<io.the.orm.UniqueConstraintViolatedException> {
                repo.create(user)
            }.and {
                get { message }.isNotNull().endsWith("field:email value:email")
                get { field }.isEqualTo(User::email)
                get { fieldValue }.isEqualTo("email")
            }
        }
    }
}
