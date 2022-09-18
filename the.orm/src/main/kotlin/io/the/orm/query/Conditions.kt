package io.the.orm.query

import java.time.LocalDate
import kotlin.reflect.KProperty1

fun <T : Any, V> KProperty1<T, V>.isNull(): QueryFactory.Condition<Unit> =
    QueryFactory.isNullCondition(this)

fun <T : Any> KProperty1<T, String?>.like(): QueryFactory.Condition<String> =
    QueryFactory.likeCondition(this)

fun <T : Any, V : Any> KProperty1<T, V?>.isEqualTo(): QueryFactory.Condition<V> =
    QueryFactory.isEqualToCondition(this)

fun <T : Any> KProperty1<T, LocalDate?>.between(): QueryFactory.Condition<Pair<LocalDate, LocalDate>> =
    QueryFactory.Condition("between ? and ?", this)
