package r2dbcfun.internal

import r2dbcfun.PropertyReader
import r2dbcfun.util.toSnakeCase

internal class PropertiesReader<T>(val propertyReaders: List<PropertyReader<T>>) {
    val types: List<Class<*>> = propertyReaders.map { it.dbClass }
    val fieldNames: String = propertyReaders.joinToString { it.name.toSnakeCase() }

    fun values(instance: T): Sequence<Any?> {
        return propertyReaders.asSequence().map { it.value(instance) }
    }
}
