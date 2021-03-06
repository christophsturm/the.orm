package r2dbcfun.internal

import r2dbcfun.util.toSnakeCase
import kotlin.reflect.KClass

class Table<T : Any>(kClass: KClass<T>) {
    val name = "${kClass.simpleName!!.toSnakeCase().toLowerCase()}s"
}
