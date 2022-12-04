package io.the.orm.query

import java.time.LocalDate
import kotlin.reflect.KProperty1

fun <T : Any, V> KProperty1<T, V>.isNotNull(): QueryFactory.Condition<Unit> =
    Conditions.isNotNullCondition(this)

fun <T : Any, V> KProperty1<T, V>.isNull(): QueryFactory.Condition<Unit> =
    Conditions.isNullCondition(this)

fun <T : Any> KProperty1<T, String?>.like(): QueryFactory.Condition<String> =
    Conditions.likeCondition(this)

fun <T : Any, V : Any> KProperty1<T, V?>.isEqualTo(): QueryFactory.Condition<V> =
    Conditions.isEqualToCondition(this)

fun <T : Any, V : Any> KProperty1<T, V?>.isIn(): QueryFactory.Condition<Array<V>> =
    QueryFactory.Condition("= ANY(?)", this)

fun <T : Any> KProperty1<T, LocalDate?>.between(): QueryFactory.Condition<Pair<LocalDate, LocalDate>> =
    QueryFactory.Condition("between ? and ?", this)

object Conditions {
    fun <T : Any, V> isNotNullCondition(property: KProperty1<T, V>): QueryFactory.Condition<Unit> =
        QueryFactory.Condition("is not null", property)
    fun <T : Any, V> isNullCondition(property: KProperty1<T, V>): QueryFactory.Condition<Unit> =
        QueryFactory.Condition("is null", property)

    fun <T : Any> likeCondition(property: KProperty1<T, String?>): QueryFactory.Condition<String> =
        QueryFactory.Condition("like(?)", property)

    fun <T : Any, V : Any> isEqualToCondition(property: KProperty1<T, V?>):
        QueryFactory.Condition<V> = QueryFactory.Condition("=?", property)
}
