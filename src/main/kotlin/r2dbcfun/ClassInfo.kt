package r2dbcfun

import java.time.LocalDate
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaType

internal class ClassInfo<T : Any>(kClass: KClass<T>) {
    companion object {
        // from the r2dbc spec: https://r2dbc.io/spec/0.8.1.RELEASE/spec/html/#datatypes
        private val supportedJavaTypes = setOf<Class<*>>(
            String::class.java,
            io.r2dbc.spi.Clob::class.java,
            Boolean::class.java,
            java.lang.Boolean::class.java, // for nullable booleans
            java.nio.ByteBuffer::class.java,
            io.r2dbc.spi.Blob::class.java,
            Int::class.java,
            Byte::class.java,
            Short::class.java,
            Long::class.java,
            LocalDate::class.java
        )

        private fun makeConverter(parameter: KParameter): FieldConverter {
            val clazz = parameter.type.javaType as Class<*>
            return if (clazz.isEnum)
                EnumConverter(clazz)
            else {
                if (parameter.name != "id" // Primary key can be a pk class which is currently not handled here
                    && !supportedJavaTypes.contains(clazz)
                )
                    throw R2dbcRepoException("type ${clazz.simpleName} not supported")
                PassthroughFieldConverter()
            }
        }
    }

    data class FieldInfo(
        val constructorParameter: KParameter, val snakeCaseName: String, val fieldConverter: FieldConverter
    ) {
        constructor(parameter: KParameter) : this(parameter, parameter.name!!.toSnakeCase(), makeConverter(parameter))
    }

    val constructor: KFunction<T> = kClass.primaryConstructor
        ?: throw RuntimeException("No primary constructor found for ${kClass.simpleName}")

    val fieldInfo = constructor.parameters.map { FieldInfo(it) }


}

/**
 * converts strings from the database to enums in the mapped class
 */
internal class EnumConverter(private val clazz: Class<*>) : FieldConverter {
    override fun valueToConstructorParameter(value: Any?): Any? {
        if (value == null)
            return null

        @Suppress("UPPER_BOUND_VIOLATED", "UNCHECKED_CAST", "RemoveExplicitTypeArguments")
        return (java.lang.Enum.valueOf<Any>(clazz as Class<Any>, value as String))
    }
}

/**
 * converter for fields that need no conversion
 */
internal class PassthroughFieldConverter : FieldConverter {
    override fun valueToConstructorParameter(value: Any?): Any? = value
}

internal interface FieldConverter {
    fun valueToConstructorParameter(value: Any?): Any?

}
