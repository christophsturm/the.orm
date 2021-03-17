package r2dbcfun.exp.ar

import failfast.FailFast
import failfast.describe
import r2dbcfun.RepositoryImpl
import r2dbcfun.dbio.ConnectionProvider
import r2dbcfun.test.DBS
import r2dbcfun.test.forAllDatabases
import r2dbcfun.test.functional.Color
import r2dbcfun.test.functional.UserPK
import java.math.BigDecimal
import java.time.LocalDate

/*
lay out how an active record api could look like
 */
data class User(
    val id: UserPK? = null,
    val name: String,
    val email: String?,
    val isCool: Boolean? = false,
    val bio: String? = null,
    val favoriteColor: Color? = null,
    val birthday: LocalDate? = null,
    val weight: Double? = null,
    val balance: BigDecimal? = null
) : ActiveRecord

interface ActiveRecord

fun main() {
    FailFast.runTest()
}

object ActiveRecordFunctionalTest {

    val context = describe("Active Record API") {
        forAllDatabases(DBS.databases) { connectionProvider ->
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

suspend inline fun <reified T : ActiveRecord> T.create(connection: ConnectionProvider): T =
    RepositoryImpl(T::class).create(connection, this)

