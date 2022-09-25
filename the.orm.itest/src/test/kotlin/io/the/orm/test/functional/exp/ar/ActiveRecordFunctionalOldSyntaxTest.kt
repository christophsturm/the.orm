package io.the.orm.test.functional.exp.ar

import failgood.describe
import io.the.orm.test.DBS
import io.the.orm.test.forAllDatabases
import io.the.orm.test.functional.USERS_SCHEMA
import java.time.LocalDate

/**
 * leaving this here as an example for [forAllDatabases]
 */

object ActiveRecordFunctionalOldSyntaxTest {

    val context = describe("Active Record API", disabled = true) {
        forAllDatabases(DBS.databases, USERS_SCHEMA) { connectionProvider ->
            val connection = connectionProvider()
            it("just works") {
                val user = User(
                    name = "chris",
                    email = "email",
                    birthday = LocalDate.parse("2020-06-20")
                )
                user.create(connection)
            }
        }
    }
}
