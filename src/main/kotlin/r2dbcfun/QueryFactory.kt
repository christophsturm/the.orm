package r2dbcfun

import io.r2dbc.spi.Connection
import java.time.LocalDate
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlinx.coroutines.flow.Flow

public fun <T : Any, V> KProperty1<T, V>.isNull(): QueryFactory.Condition<Unit> =
    QueryFactory.isNullCondition(this)

public fun <T : Any> KProperty1<T, String?>.like(): QueryFactory.Condition<String> =
    QueryFactory.likeCondition(this)

public fun <T : Any, V : Any> KProperty1<T, V?>.equals(): QueryFactory.Condition<V> =
    QueryFactory.equalsCondition(this)

public fun <T : Any> KProperty1<T, LocalDate?>.between():
    QueryFactory.Condition<Pair<LocalDate, LocalDate>> =
        QueryFactory.Condition("between ? and ?", this)

public class QueryFactory<T : Any> internal constructor(
    kClass: KClass<T>,
    private val finder: Finder<T>
) {
    public companion object {
        public fun <T : Any, V> isNullCondition(property: KProperty1<T, V>): Condition<Unit> =
            Condition("is null", property)

        public fun <T : Any> likeCondition(property: KProperty1<T, String?>): Condition<String> =
            Condition("like(?)", property)

        public fun <T : Any, V : Any> equalsCondition(property: KProperty1<T, V?>): Condition<V> =
            Condition("=?", property)
    }

    private val snakeCaseForProperty =
        kClass.declaredMemberProperties.associateBy({ it }, { it.name.toSnakeCase() })

    @Suppress("SqlResolve")
    private val tableName = "${kClass.simpleName!!.toSnakeCase().toLowerCase()}s"

    private val selectPrefix =
        "select ${snakeCaseForProperty.values.joinToString { it }} from $tableName where "
    public fun <P1 : Any> createQuery(p1: Condition<P1>): OneParameterQuery<P1> =
        OneParameterQuery(p1)

    public fun <P1 : Any, P2 : Any> createQuery(p1: Condition<P1>, p2: Condition<P2>):
        TwoParameterQuery<P1, P2> = TwoParameterQuery(p1, p2)

    @Suppress("unused")
    public data class Condition<Type>(val conditionString: String, val prop: KProperty1<*, *>)

    public inner class OneParameterQuery<P1 : Any>(p1: Condition<P1>) {
        private val query = Query(p1)
        public suspend operator fun invoke(connection: Connection, p1: P1): Flow<T> =
            query.find(connection, p1)
    }

    public inner class TwoParameterQuery<P1 : Any, P2 : Any>(p1: Condition<P1>, p2: Condition<P2>) {
        private val query = Query(p1, p2)
        public suspend operator fun invoke(connection: Connection, p1: P1, p2: P2): Flow<T> =
            query.find(connection, p1, p2)
    }

    // internal api
    public inner class Query internal constructor(private vararg val conditions: Condition<*>) {
        private val selectString =
            run {
                val queryString =
                    conditions.joinToString(separator = " and ") {
                        "${snakeCaseForProperty[it.prop]} ${it.conditionString}"
                    }
                selectPrefix + queryString
            }.toIndexedPlaceholders()

        public suspend fun find(connection: Connection, vararg parameter: Any): Flow<T> {
            val parameterValues =
                parameter.asSequence()
                    .flatMap {
                        if (it is Pair<*, *>)
                            sequenceOf(it.first as Any, it.second as Any)
                        else
                            sequenceOf(it)
                    }
            return finder.findBy(connection, selectString, parameterValues)
        }
    }
}
