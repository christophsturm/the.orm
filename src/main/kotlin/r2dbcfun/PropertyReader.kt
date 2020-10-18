package r2dbcfun

import io.r2dbc.spi.Statement
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubclassOf

internal class PropertyReader<T>(private val property: KProperty1<T, *>) {
    val name = property.name
    private val kClass = property.returnType.classifier as KClass<*>

    internal fun bindValueOrNull(
        statement: Statement,
        index: Int,
        instance: T
    ): Statement {
        val value = property.call(instance)
        return try {
            if (value == null) {
                val clazz = if (kClass.isSubclassOf(Enum::class)) String::class.java else kClass.java
                statement.bindNull(index, clazz)
            } else {

                statement.bind(index, if (value::class.isSubclassOf(Enum::class)) value.toString() else value)
            }
        } catch (e: java.lang.IllegalArgumentException) {
            throw R2dbcRepoException(
                "error binding value $value to field $name with index $index",
                e
            )
        }
    }

}
