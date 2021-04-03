package r2dbcfun.exp.ar

import failfast.FailFast
import r2dbcfun.RepositoryImpl
import r2dbcfun.dbio.ConnectionProvider
import r2dbcfun.test.DBS
import r2dbcfun.test.describeOnAllDbs
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

    val context = describeOnAllDbs("Active Record API", DBS.databases) { connectionProvider ->
        it("just works") {
            val connection = connectionProvider()
            val user = User(
                name = "chris",
                email = "email",
                birthday = LocalDate.parse("2020-06-20")
            )
            user.create(connection)
        }
    }
}

suspend inline fun <reified T : ActiveRecord> T.create(connection: ConnectionProvider): T =
    RepositoryImpl(T::class).create(connection, this)

