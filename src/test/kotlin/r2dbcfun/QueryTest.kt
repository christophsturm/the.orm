package r2dbcfun

import dev.minutest.experimental.SKIP
import dev.minutest.experimental.minus
import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import io.r2dbc.spi.Connection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

@ExperimentalCoroutinesApi
class QueryTest : JUnit5Minutests {

    @Suppress("unused")
    fun tests() = rootContext<Unit> {
        context("query") {
            val date1 = LocalDate.now()
            val date2 = LocalDate.now()
            test("first query api") {
                runBlocking {
                    val connection = prepareH2().create().awaitSingle()
                    val users: Flow<User> =
                        SelectFrom<User>(connection).where(
                            User::name.like("blah%").and(User::birthday.between(date1, date2))
                        ).asFlow()
                }
            }
            SKIP - test("other query api") {
                Find2<User>() {
                    User::name.like("blah%")
                    and(User::birthday.between2(date1, date2))
                }
            }
        }
    }
}

private fun <T, V> KProperty1<T, V>.between2(date1: V, date2: V): V? {
    TODO("blah")
}


private inline fun <reified T : Any> SelectFrom(connection: Connection) = FindThing(T::class, connection)


private fun <T, V> KProperty1<T, V>.between(date1: V, date2: V) = BetweenCondition(this, date1, date2)

class BetweenCondition<T, V>(kProperty1: KProperty1<T, V>, date1: V, date2: V) {

}

private fun <T, V> KProperty1<T, V>.like(v: V): QueryBuilder = QueryBuilder()

class QueryBuilder {
    fun and(between: Any?): QueryBuilder = this
    infix fun and(between: LocalDate?): QueryBuilder = this

}

class FindThing<T : Any>(val kClass: KClass<T>, connection: Connection) {
    private val repo = R2dbcRepo(connection, kClass)
    fun where(and: QueryBuilder) = this
    fun asFlow(): Flow<T> {
        TODO("Not yet implemented")
    }

}

private fun <T> Find2(function: FindThing2.() -> Unit) {
    TODO("Not yet implemented")
}

class FindThing2 {
    fun where(and: QueryBuilder) {
        TODO("Not yet implemented")
    }

    fun and(between: LocalDate?) {
        TODO("Not yet implemented")
    }

}
