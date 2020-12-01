package r2dbcfun

import r2dbcfun.util.toSnakeCase
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaType

// from the r2dbc spec: https://r2dbc.io/spec/0.8.1.RELEASE/spec/html/#datatypes
private val supportedTypes =
    setOf<KClass<*>>(
        String::class,
        io.r2dbc.spi.Clob::class,
        Boolean::class,
        java.nio.ByteBuffer::class,
        io.r2dbc.spi.Blob::class,
        Int::class,
        Byte::class,
        Short::class,
        Long::class,
        Double::class,
        BigDecimal::class,
        LocalDate::class
    )

private fun makeConverter(parameter: KParameter): FieldConverter {
    val type = parameter.type
    val javaClass = type.javaType as Class<*>
    val kotlinClass = type.classifier as KClass<*>
    return when {
        javaClass.isEnum -> EnumConverter(javaClass)
        kotlinClass == Double::class -> FieldConverter { (it as Number?)?.toDouble() }
        else -> {
            val isPK = parameter.name == "id"
            if (isPK || supportedTypes.contains(kotlinClass))
                FieldConverter { it }
            else
                throw RepositoryException("type ${kotlinClass.simpleName} not supported")
        }
    }
}

internal class ClassInfo<T : Any>(kClass: KClass<T>) {
    val constructor: KFunction<T> =
        kClass.primaryConstructor
            ?: throw RuntimeException("No primary constructor found for ${kClass.simpleName}")

    val fieldInfo = constructor.parameters.map { FieldInfo(it) }

    data class FieldInfo(
        val constructorParameter: KParameter,
        val snakeCaseName: String,
        val fieldConverter: FieldConverter
    ) {
        constructor(parameter: KParameter) :
                this(parameter, parameter.name!!.toSnakeCase(), makeConverter(parameter))
    }
}


/** converts strings from the database to enums in the mapped class */
private class EnumConverter(private val clazz: Class<*>) : FieldConverter {
    override fun valueToConstructorParameter(value: Any?): Any? {
        if (value == null) return null

        @Suppress("UPPER_BOUND_VIOLATED", "UNCHECKED_CAST", "RemoveExplicitTypeArguments")
        return (java.lang.Enum.valueOf<Any>(clazz as Class<Any>, value as String))
    }
}

internal fun interface FieldConverter {
    fun valueToConstructorParameter(value: Any?): Any?
}
