package io.the.orm.internal

import failgood.Test
import failgood.describe
import io.r2dbc.spi.R2dbcDataIntegrityViolationException
import io.the.orm.UniqueConstraintViolatedException
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo

@Test
class ExceptionInspectorTest {
    data class User(val name: String, val email: String?)

    val context = describe(ExceptionInspector::class) {
        it("works") {
            val user = User(
                name = "chris",
                email = "email-value"
            )

            val message =
                "Eindeutiger Index oder Primärschlüssel verletzt: \"PUBLIC.CONSTRAINT_INDEX_4 ON PUBLIC.USERS(EMAIL NULLS FIRST) VALUES ( /* 1 */ 'email' )\"\n" +
                        "Unique index or primary key violation: \"PUBLIC.CONSTRAINT_INDEX_4 ON PUBLIC.USERS(EMAIL NULLS FIRST) VALUES ( /* 1 */ 'email' )\"; SQL statement:\n" +
                        "INSERT INTO users(name, email, is_cool, bio, favorite_color, birthday, weight, balance) values (\$1, \$2, \$3, \$4, \$5, \$6, \$7, \$8) [23505-206]"
            val result = ExceptionInspector(Table("users"), User::class).r2dbcDataIntegrityViolationException(
                R2dbcDataIntegrityViolationException(message), user
            )
            expectThat(result).isA<UniqueConstraintViolatedException>().and {
                get { field }.isEqualTo(User::email)
                get { fieldValue }.isEqualTo("email-value")

            }
        }
    }
}
