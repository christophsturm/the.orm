package r2dbcfun.exp.query

// one of the query languages that  did not like so much in the end.

import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import r2dbcfun.User
import r2dbcfun.exp.query.Query.Condition
import r2dbcfun.toSnakeCase
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.LocalDate
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

@ExperimentalCoroutinesApi
class QueryTest : JUnit5Minutests {

    @Suppress("unused")
    fun tests() = rootContext<Unit> {
        context("query") {
            val date1 = LocalDate.now()
            val date2 = LocalDate.now()
            test("it generates sql from a query") {
                val query = find(User::name.like("blah%"), User::birthday.between(date1, date2))
                expectThat(query.queryString).isEqualTo("name like(?) and birthday between ? and ?")
            }
        }
    }
}


private inline fun <reified T : Any> find(vararg conditions: Condition<T, *>) = Query(T::class, conditions.toList())


private fun <T, V> KProperty1<T, V>.between(date1: V, date2: V) =
    Condition(this, "between ? and ?", listOf(date1, date2))


private fun <T, V> KProperty1<T, V>.like(v: V): Condition<T, V> = Condition(this, "like(?)", listOf(v))


class Query<T : Any>(kClass: KClass<T>, conditions: List<Condition<T, *>>) {
    private val snakeCaseForProperty =
        kClass.declaredMemberProperties.associateBy({ it }, { it.name.toSnakeCase() })

    data class Condition<T, V>(val property: KProperty1<T, V>, val queryFragment: String, val parameters: List<Any?>)

    internal val queryString =
        conditions.joinToString(separator = " and ") { "${snakeCaseForProperty[it.property]} ${it.queryFragment}" }
//    private val parameters = conditions.flatMap { it.parameters }
/*    suspend fun fromConnection(connection: Connection): Flow<T> {
        return finder.findBy(connection, queryString, parameters)
    }*/
}


