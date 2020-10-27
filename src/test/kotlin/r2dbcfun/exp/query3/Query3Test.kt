package r2dbcfun.exp.query3

import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import r2dbcfun.User
import java.time.LocalDate
import kotlin.reflect.KProperty1

@ExperimentalCoroutinesApi
class Query3Test : JUnit5Minutests {

    @Suppress("unused")
    fun tests() = rootContext<Unit> {
        context("query") {
            val date1 = LocalDate.now()
            val date2 = LocalDate.now()
            test("first query api") {
                val query = Query(User::name.like(), User::birthday.between())
                query.find("blah%", Pair(date1, date2))
            }
        }
    }
}

private fun <T : Any, V> KProperty1<T, V>.like() = Condition<V>(this)

private fun <T : Any, V> KProperty1<T, V>.between() = Condition<Pair<LocalDate, LocalDate>>(this)

class Condition<Type>(prop: KProperty1<*, *>)

class Query<P1, P2>(p1: Condition<P1>, p2: Condition<P2>) {
    fun find(p1: P1, p2: P2) {}
}


