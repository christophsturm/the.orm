@file:Suppress("unused", "UNUSED_PARAMETER")

// this is just for playing around with a new query language. it compiles but does not run.
// just pretend that it does not exist.

package r2dbcfun.exp

import dev.minutest.experimental.SKIP
import dev.minutest.experimental.minus
import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import r2dbcfun.User
import java.time.LocalDate
import kotlin.reflect.KProperty1

class Query2Test : JUnit5Minutests {

    @Suppress("unused")
    fun tests() = rootContext<Unit> {
        SKIP - context("query") {
            val date1 = LocalDate.now()
            val date2 = LocalDate.now()
            test("other query api") {
                find<User> {
                    User::name.like("blah%")
                    User::birthday.between2(date1, date2)
                }
            }
        }
    }
}

private fun <T, V> KProperty1<T, V>.between2(date1: V, date2: V): V? {
    TODO("blah")
}


private fun <T, V> KProperty1<T, V>.between(date1: V, date2: V) = BetweenCondition(this, date1, date2)

class BetweenCondition<T, V>(kProperty1: KProperty1<T, V>, date1: V, date2: V)

private fun <T, V> KProperty1<T, V>.like(v: V): QueryBuilder = QueryBuilder()

class QueryBuilder {
    fun and(between: Any?): QueryBuilder = this
    infix fun and(between: LocalDate?): QueryBuilder = this

}

private fun <T> find(function: FindThing2.() -> Unit) {
    TODO("Not yet implemented")
}

class FindThing2 {
    fun and(between: LocalDate?) {
        TODO("Not yet implemented")
    }

}

