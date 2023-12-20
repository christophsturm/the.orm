package io.the.orm.mapper

import io.the.orm.dbio.LazyResult

internal data class ResultLine(val fields: List<Any?>, val relations: List<Any?>)

internal data class LazyResultLine(
    val fields: List<LazyResult<*>>,
    val relations: List<LazyResult<*>>
)
