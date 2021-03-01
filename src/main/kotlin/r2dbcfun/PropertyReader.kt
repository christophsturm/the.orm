package r2dbcfun

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubclassOf

internal class PropertyReader<T>(private val property: KProperty1<T, *>) {
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
