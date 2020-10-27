package r2dbcfun.query

import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import io.r2dbc.spi.Connection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import r2dbcfun.User
import r2dbcfun.prepareH2
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.LocalDate
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

@ExperimentalCoroutinesApi
class Query3Test : JUnit5Minutests {

    @Suppress("unused")
    fun tests() = rootContext<Unit> {
        context("query") {
            val date1 = LocalDate.now()
            val date2 = LocalDate.now()
            test("first query api") {
                val query = query(User::class, User::name.like(), User::birthday.between())
                expectThat(query.selectString).isEqualTo("select id, name, email, is_cool, bio, favorite_color, birthday from users where name like(?) and birthday between ? and ?")
                runBlocking {
                    val connection = prepareH2().create().awaitSingle()
                    query.find(connection, "blah%", Pair(date1, date2))
                }
            }
        }
    }

}

private fun query(kClass: KClass<User>, p1: Condition<String>, p2: Condition<Pair<LocalDate, LocalDate>>) =
    Query2P(kClass, p1, p2)

private fun <T : Any> KProperty1<T, String>.like() = Condition<String>("like(?)", this)

private fun <T : Any> KProperty1<T, LocalDate>.between() =
    Condition<Pair<LocalDate, LocalDate>>("between ? and ?", this)

data class Condition<Type>(val conditionString: String, val prop: KProperty1<*, *>)

class Query2P<T : Any, P1 : Any, P2 : Any>(kClass: KClass<T>, p1: Condition<P1>, p2: Condition<P2>) :
    Query<T>(kClass, p1, p2) {
    suspend fun find(connection: Connection, p1: P1, p2: P2) = super.find(connection, p1, p2)
}


