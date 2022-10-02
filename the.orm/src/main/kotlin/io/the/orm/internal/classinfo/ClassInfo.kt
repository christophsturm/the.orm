package io.the.orm.internal.classinfo

import io.r2dbc.spi.Blob
import io.r2dbc.spi.Clob
import io.the.orm.RepositoryException
import io.the.orm.exp.relations.BelongsTo
import io.the.orm.exp.relations.HasMany
import io.the.orm.internal.IDHandler
import io.the.orm.util.toSnakeCase
import io.vertx.sqlclient.data.Numeric
import java.lang.reflect.ParameterizedType
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

    val fieldInfo: List<FieldInfo> = constructor.parameters.map { parameter ->
        fieldInfo(parameter, otherClasses, idHandler)
    }

    private fun fieldInfo(
        parameter: KParameter,
        otherClasses: Set<KClass<*>>,
        idHandler: IDHandler<T>
    ): FieldInfo {
        val type = parameter.type
        val javaClass = when (val t = type.javaType) {
            is Class<*> -> t
            is ParameterizedType -> t.actualTypeArguments.single() as Class<*>
            else -> throw RuntimeException("unsupported type: ${t.typeName}")
        }
        val kotlinClass = when (val kc = type.classifier as KClass<*>) {
            BelongsTo::class, HasMany::class -> type.arguments.single().type!!.classifier as KClass<*>
            else -> kc
        }
        val fieldName = parameter.name!!.toSnakeCase()
        val property = properties[parameter.name]!!

        return if (otherClasses.contains(kotlinClass)) {
            FieldInfo(
                parameter, property, fieldName + "_id",
                BelongsToConverter(IDHandler(kotlinClass)), Long::class.java, true
            )
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

    val propertyToFieldInfo = fieldInfo.associateBy { it.property }
    val partitions = fieldInfo.partition { it.isRelation }
    val fields = partitions.first
    val relations = partitions.second

    val hasRelations = fieldInfo.any { it.isRelation }
    fun values(instance: T): Sequence<Any?> {
        return fieldInfo.asSequence().map { it.valueForDb(instance) }
    }

    data class FieldInfo(
        val constructorParameter: KParameter,
        val property: KProperty1<*, *>,
        val dbFieldName: String,
        val fieldConverter: FieldConverter,
        val type: Class<*>,
        val isRelation: Boolean = false
    ) {
        fun valueForDb(instance: Any): Any? = fieldConverter.propertyToDBValue(property.call(instance))
    }
}

private class PKFieldConverter(val idHandler: IDHandler<*>) : FieldConverter {
    override fun dbValueToParameter(value: Any?) = idHandler.createId(value as Long)

    override fun propertyToDBValue(value: Any?): Any? = value?.let { idHandler.getId(it) }
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
