package io.the.orm.internal

import io.the.orm.util.toSnakeCase
import java.util.Locale
import kotlin.reflect.KClass

class Table(val baseName: String) {
    val name = baseName + "s"
    constructor(kClass: KClass<*>) : this("${kClass.simpleName!!.toSnakeCase().lowercase(Locale.getDefault())}")
}
