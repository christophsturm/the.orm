package r2dbcfun

import r2dbcfun.query.Condition
import r2dbcfun.query.TwoParameterQuery
import kotlin.reflect.KClass

public class QueryFactory<T : Any>(tableName: String, private val kClass: KClass<T>) {
    public fun <P1 : Any, P2 : Any> query(p1: Condition<P1>, p2: Condition<P2>): TwoParameterQuery<T, P1, P2> =
        TwoParameterQuery(kClass, p1, p2)

}
