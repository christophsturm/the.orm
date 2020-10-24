package r2dbcfun

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
                        selectFrom<User>().where(
                            User::name.like("blah%").and(User::birthday.between(date1, date2))
                        ).fromConnection(connection)
                }
            }
            test("it generates sql from a query") {
                val query = selectFrom<User>().where(
                    User::name.like("blah%").and(User::birthday.between(date1, date2))
                )
            }
        }
    }
}

private fun <T, V> KProperty1<T, V>.between2(date1: V, date2: V): V? {
    TODO("blah")
}


private inline fun <reified T : Any> selectFrom() = Query(T::class)


private fun <T, V> KProperty1<T, V>.between(date1: V, date2: V) = BetweenCondition(this, date1, date2)

class BetweenCondition<T, V>(kProperty1: KProperty1<T, V>, date1: V, date2: V)

private fun <T, V> KProperty1<T, V>.like(v: V): QueryBuilder = QueryBuilder()

class QueryBuilder {
    fun and(between: Any?): QueryBuilder = this
    infix fun and(between: LocalDate?): QueryBuilder = this

}

class Query<T : Any>(kClass: KClass<T>) {
    private val finder = R2dbcRepo(kClass).finder
    fun where(and: QueryBuilder) = this
    suspend fun fromConnection(connection: Connection): Flow<T> {
        val queryString = "1=1"
        val parameters = listOf<Any>()
        return finder.findBy(connection, queryString, parameters)
    }

}

