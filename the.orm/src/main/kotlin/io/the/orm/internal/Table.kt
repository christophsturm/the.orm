package io.the.orm.internal

import io.the.orm.util.toSnakeCase
import kotlin.reflect.KClass

class Table(val name: String) {
    constructor(kClass: KClass<*>) : this("${kClass.simpleName!!.toSnakeCase().toLowerCase()}s")
}
