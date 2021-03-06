package r2dbcfun.internal

import r2dbcfun.util.toSnakeCase
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubclassOf

internal class PropertiesReader<T>(properties: Map<String, KProperty1<T, *>>) {
    private val propertyReaders: List<PropertyReader<T>> =
        properties.filter { it.key != "id" }.values.map { PropertyReader(it) }
    val types: List<Class<*>> = propertyReaders.map { it.dbClass }
    val dbFieldNames: List<String> = propertyReaders.map { it.name.toSnakeCase() }

    fun values(instance: T): Sequence<Any?> {
        return propertyReaders.asSequence().map { it.value(instance) }
    }

    private class PropertyReader<T>(private val property: KProperty1<T, *>) {
        val name = property.name
        private val kClass = property.returnType.classifier as KClass<*>

        private val isEnum = kClass.isSubclassOf(Enum::class)

        // enums are strings from the dbs view
        val dbClass = if (isEnum) String::class.java else kClass.java
        fun value(instance: T): Any? {
            val value = property.call(instance)
            return if (value != null && isEnum) value.toString() else value

        }
    }

}
