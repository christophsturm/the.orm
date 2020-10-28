package r2dbcfun

import io.r2dbc.spi.Connection
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

public fun <T : Any> KProperty1<T, String>.like(): QueryFactory.Condition<String> =
    QueryFactory.likeCondition(this)

public fun <T : Any, V> KProperty1<T, V>.equals(): QueryFactory.Condition<V> =
    QueryFactory.equalsCondition(this)

public fun <T : Any> KProperty1<T, LocalDate>.between(): QueryFactory.Condition<Pair<LocalDate, LocalDate>> =
    QueryFactory.Condition("between ? and ?", this)


public class QueryFactory<T : Any> internal constructor(private val kClass: KClass<T>, private val finder: Finder<T>) {
    public companion object {
        public fun <T : Any> likeCondition(property: KProperty1<T, String>): Condition<String> =
            Condition("like(?)", property)

        public fun <T : Any, V> equalsCondition(property: KProperty1<T, V>): Condition<V> =
            Condition("=?", property)

    }

    public fun <P1 : Any, P2 : Any> query(p1: Condition<P1>, p2: Condition<P2>): TwoParameterQuery<P1, P2> =
        TwoParameterQuery(kClass, p1, p2)


    @Suppress("unused")
    public data class Condition<Type>(val conditionString: String, val prop: KProperty1<*, *>)

    public inner class TwoParameterQuery<P1 : Any, P2 : Any>(
        kClass: KClass<T>,
        p1: Condition<P1>,
        p2: Condition<P2>
    ) {
        private val query = Query(kClass, finder, p1, p2)
        public suspend fun find(connection: Connection, p1: P1, p2: P2): Flow<T> = query.find(connection, p1, p2)
    }


    // internal api
    public class Query<T : Any> internal constructor(
        kClass: KClass<T>,
        private val finder: Finder<T>,
        private vararg val conditions: Condition<*>
    ) {
        private val snakeCaseForProperty =
            kClass.declaredMemberProperties.associateBy({ it }, { it.name.toSnakeCase() })

        @Suppress("SqlResolve")
        private val tableName = "${kClass.simpleName!!.toSnakeCase().toLowerCase()}s"

        private val selectString = run {

            val queryString =
                conditions.joinToString(separator = " and ") { "${snakeCaseForProperty[it.prop]} ${it.conditionString}" }
            "select ${snakeCaseForProperty.values.joinToString { it }} from $tableName where " + queryString
        }.toIndexedPlaceholders()


        public suspend fun find(connection: Connection, vararg parameter: Any): Flow<T> =
            finder.findBy(parameter.asList(), connection, selectString)

    }


}
