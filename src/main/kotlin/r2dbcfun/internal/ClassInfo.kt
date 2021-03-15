package r2dbcfun.internal

import io.r2dbc.spi.Blob
import io.r2dbc.spi.Clob
import io.vertx.sqlclient.data.Numeric
import r2dbcfun.Repository
import r2dbcfun.RepositoryException
import r2dbcfun.util.toSnakeCase
import java.math.BigDecimal
import java.nio.ByteBuffer
import java.time.LocalDate
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaType

private val passThroughFieldConverter = PassThroughConverter

internal interface FieldConverter {
    fun dbValueToParameter(value: Any?): Any? = value
    fun propertyToDBValue(value: Any?): Any? = value
}

object PassThroughConverter : FieldConverter

// from the r2dbc spec: https://r2dbc.io/spec/0.8.4.RELEASE/spec/html/#datatypes
private val fieldConverters =
    mapOf<KClass<*>, FieldConverter>(
        String::class to passThroughFieldConverter,
        Clob::class to passThroughFieldConverter,
        Boolean::class to passThroughFieldConverter,
        ByteBuffer::class to passThroughFieldConverter,
        Blob::class to passThroughFieldConverter,
        Int::class to passThroughFieldConverter,
        Byte::class to passThroughFieldConverter,
        Short::class to passThroughFieldConverter,
        Long::class to passThroughFieldConverter,
        Double::class to DoubleConverter,
        BigDecimal::class to BigDecimalConverter,
        LocalDate::class to passThroughFieldConverter
    )

object BigDecimalConverter : FieldConverter {
    override fun dbValueToParameter(value: Any?): Any? {
        return if (value is Numeric) value.bigDecimalValue() else value
    }
}

object DoubleConverter : FieldConverter {
    override fun dbValueToParameter(value: Any?): Any? {
        return (value as Number?)?.toDouble()
    }
}


internal class ClassInfo<T : Any>(
    kClass: KClass<T>,
    idHandler: IDHandler<T>,
    otherClasses: Set<KClass<*>>
) {
    private val properties: Map<String, KProperty1<T, *>> =
        kClass.declaredMemberProperties.associateBy({ it.name }, { it })

    val name = kClass.simpleName
    val constructor: KFunction<T> =
        kClass.primaryConstructor
            ?: throw RuntimeException("No primary constructor found for ${kClass.simpleName}")

    val fieldInfo = constructor.parameters.map { parameter ->
        val type = parameter.type
        val javaClass = type.javaType as Class<*>
        val kotlinClass = type.classifier as KClass<*>
        val fieldName = parameter.name!!.toSnakeCase()
        val property = properties[parameter.name]!!

        if (otherClasses.contains(kotlinClass)) {
            FieldInfo(parameter, property, fieldName + "_id", BelongsToConverter(kotlinClass), Long::class.java)
        } else when {
            javaClass.isEnum -> FieldInfo(
                parameter,
                property,
                fieldName,
                EnumConverter(javaClass),
                String::class.java
            )
            else -> {
                val isPK = parameter.name == "id"
                if (isPK) FieldInfo(
                    parameter,
                    property,
                    fieldName,
                    PKFieldConverter(idHandler),
                    Long::class.java
                )
                else {
                    val fieldConverter = fieldConverters[kotlinClass]
                        ?: throw RepositoryException("type ${kotlinClass.simpleName} not supported")
                    FieldInfo(parameter, property, fieldName, fieldConverter, javaClass)
                }
            }
        }
    }

    fun values(instance: T): Sequence<Any?> {
        return fieldInfo.asSequence().map { it.value(instance) }
    }

    data class FieldInfo(
        val constructorParameter: KParameter,
        val property: KProperty1<*, *>,
        val dbFieldName: String,
        val fieldConverter: FieldConverter,
        val type: Class<*>
    ) {
        fun value(instance: Any): Any? = fieldConverter.propertyToDBValue(property.call(instance))
    }

}

private class PKFieldConverter(val idHandler: IDHandler<*>) : FieldConverter {
    override fun dbValueToParameter(value: Any?) = idHandler.createId(value as Long)

    override fun propertyToDBValue(value: Any?): Any? = value?.let { idHandler.getId(it) }
}

class BelongsToConverter(kotlinClass: KClass<*>) : FieldConverter {
    val repo = Repository(kotlinClass)

    override fun dbValueToParameter(value: Any?): Any? {
        TODO("Not yet implemented")
    }

    override fun propertyToDBValue(value: Any?): Any? {
        TODO("Not yet implemented")
    }

}


/** converts strings from the database to enums in the mapped class */
private class EnumConverter(private val clazz: Class<*>) : FieldConverter {
    override fun dbValueToParameter(value: Any?): Any? {
        if (value == null) return null

        @Suppress("UPPER_BOUND_VIOLATED", "UNCHECKED_CAST", "RemoveExplicitTypeArguments")
        return (java.lang.Enum.valueOf<Any>(clazz as Class<Any>, value as String))
    }

    override fun propertyToDBValue(value: Any?): Any? {
        return value?.toString()
    }
}

