package r2dbcfun.internal

import r2dbcfun.util.toSnakeCase
import kotlin.reflect.KClass

class Table<T : Any>(val name: String) {
    constructor(kClass: KClass<T>) : this("${kClass.simpleName!!.toSnakeCase().toLowerCase()}s")
}
