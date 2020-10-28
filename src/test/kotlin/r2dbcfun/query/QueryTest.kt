package r2dbcfun.query

import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import r2dbcfun.User
import r2dbcfun.prepareH2
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.LocalDate

@ExperimentalCoroutinesApi
class QueryTest : JUnit5Minutests {

    @Suppress("unused")
    fun tests() = rootContext<Unit> {
        context("query") {
            val date1 = LocalDate.now()
            val date2 = LocalDate.now()
            test("first query api") {
                val query = query(User::class, User::name.like(), User::birthday.between())
                expectThat(query.query.selectString).isEqualTo("select id, name, email, is_cool, bio, favorite_color, birthday from users where name like(?) and birthday between ? and ?")
                runBlocking {
                    val connection = prepareH2().create().awaitSingle()
                    query.find(connection, "blah%", Pair(date1, date2))
                }
            }
        }
    }

}

