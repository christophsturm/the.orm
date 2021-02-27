package r2dbcfun

import r2dbcfun.r2dbc.Statement
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubclassOf

internal class PropertyReader<T>(private val property: KProperty1<T, *>) {
    val name = property.name
    private val kClass = property.returnType.classifier as KClass<*>

    private val isEnum = kClass.isSubclassOf(Enum::class)

    private val dbClass = if (isEnum) String::class.java else kClass.java
    // enums are strings from the dbs view

    /** bind this property's value to a Statement */
    internal fun bindValue(statement: Statement, index: Int, instance: T): Statement {
        val value = property.call(instance)
        return try {
            if (value == null) {
                statement.bindNull(index, dbClass)
            } else {
                statement.bind(index, if (isEnum) value.toString() else value)
            }
        } catch (e: java.lang.IllegalArgumentException) {
            throw RepositoryException(
                "error binding value $value to field $name with index $index",
                e
            )
        }
    }
}
