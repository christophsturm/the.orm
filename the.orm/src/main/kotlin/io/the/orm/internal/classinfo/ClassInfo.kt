package io.the.orm.internal.classinfo

import io.r2dbc.spi.Blob
import io.r2dbc.spi.Clob
import io.the.orm.PK
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
private val fieldConverters = mapOf<KClass<*>, FieldConverter>(
    String::class to passThroughFieldConverter,
    Clob::class to passThroughFieldConverter,
    Boolean::class to passThroughFieldConverter,
    ByteBuffer::class to passThroughFieldConverter,
    Blob::class to passThroughFieldConverter,
    Int::class to IntConverter,
    Byte::class to passThroughFieldConverter,
    Short::class to passThroughFieldConverter,
    Long::class to LongConverter,
    Double::class to DoubleConverter,
    BigDecimal::class to BigDecimalConverter,
    LocalDate::class to passThroughFieldConverter
)

object IntConverter : FieldConverter {
    override fun dbValueToParameter(value: Any?): Int? {
        return (value as Number?)?.toInt()
    }
}

object LongConverter : FieldConverter {
    override fun dbValueToParameter(value: Any?): Long? {
        return (value as Number?)?.toLong()
    }
}

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

internal data class ClassInfo<T : Any>(
    val name: String,
    val constructor: KFunction<T>,
    val localFieldInfo: List<LocalFieldInfo>,
    val fields: List<LocalFieldInfo>,
    val belongsToRelations: List<LocalFieldInfo>,
    val hasManyRelations: List<RemoteFieldInfo>
) {
    val hasBelongsToRelations = belongsToRelations.isNotEmpty()
    val hasHasManyRelations = hasManyRelations.isNotEmpty()

    sealed interface FieldInfo {
        val constructorParameter: KParameter
        val property: KProperty1<*, *>
        val fieldConverter: FieldConverter

        /**
         * then type that we request from the database.
         * Usually the same type as the field, but for relations it will be the PK type
         */
        val type: Class<*>
        val relatedClass: KClass<*>?
    }

    class RemoteFieldInfo(
        override val constructorParameter: KParameter,
        override val property: KProperty1<*, *>,
        override val fieldConverter: FieldConverter,
        override val type: Class<*>,
        override val relatedClass: KClass<*>? = null
    ) : FieldInfo

    class LocalFieldInfo(
        override val constructorParameter: KParameter,
        override val property: KProperty1<*, *>,
        val dbFieldName: String,
        override val fieldConverter: FieldConverter,
        override val type: Class<*>,
        override val relatedClass: KClass<*>? = null
    ) : FieldInfo {
        fun valueForDb(instance: Any): Any? = fieldConverter.propertyToDBValue(property.call(instance))
    }

    fun values(instance: T): Sequence<Any?> {
        return localFieldInfo.asSequence().map { it.valueForDb(instance) }
    }

    companion object {
        internal operator fun <T : Any> invoke(
            kClass: KClass<T>,
            otherClasses: Set<KClass<*>> = setOf()
        ): ClassInfo<T> {
            val properties: Map<String, KProperty1<T, *>> =
                kClass.declaredMemberProperties.associateBy({ it.name }, { it })

            val name = kClass.simpleName
            val constructor: KFunction<T> = kClass.primaryConstructor
                ?: throw RuntimeException("No primary constructor found for ${kClass.simpleName}")

            val fieldInfo: List<FieldInfo> = constructor.parameters.map { parameter ->
                val type = parameter.type
                val kc = type.classifier as KClass<*>
                val kotlinClass = when (kc) {
                    BelongsTo::class, HasMany::class -> type.arguments.single().type!!.classifier as KClass<*>
                    else -> kc
                }
                val javaClass = when (val t = type.javaType) {
                    is Class<*> -> t
                    is ParameterizedType -> t.actualTypeArguments.single() as Class<*>
                    else -> throw RuntimeException("unsupported type: ${t.typeName}")
                }

                val fieldName = parameter.name!!.toSnakeCase()
                val property = properties[parameter.name]!!
                if (kc == HasMany::class) RemoteFieldInfo(
                    parameter, property, HasManyConverter(), Long::class.java, kotlinClass
                )
                else if (otherClasses.contains(kotlinClass)) {
                    LocalFieldInfo(
                        parameter,
                        property,
                        fieldName + "_id",
                        BelongsToConverter(IDHandler(kotlinClass)),
                        Long::class.java,
                        kotlinClass
                    )
                } else when {
                    javaClass.isEnum -> LocalFieldInfo(
                        parameter, property, fieldName, EnumConverter(javaClass), String::class.java
                    )

                    else -> {
                        val isPK = parameter.name == "id"
                        if (isPK) {
                            LocalFieldInfo(
                                parameter, property, fieldName, PKFieldConverter(IDHandler(kClass)), Long::class.java
                            )
                        } else {
                            val fieldConverter = fieldConverters[kotlinClass] ?: throw RepositoryException(
                                "type ${kotlinClass.simpleName} not supported." +
                                    " class: ${kClass.simpleName}," +
                                    " otherClasses: ${otherClasses.map { it.simpleName }}"
                            )
                            LocalFieldInfo(
                                parameter, property, fieldName, fieldConverter, javaClass
                            )
                        }
                    }
                }
            }
            val localFieldInfo = fieldInfo.filterIsInstance<LocalFieldInfo>()
            val partitions = localFieldInfo.partition { it.relatedClass != null }
            val fields = partitions.second
            val relations = partitions.first
            return ClassInfo(
                name!!, constructor, localFieldInfo, fields, relations, fieldInfo.filterIsInstance<RemoteFieldInfo>()
            )
        }
    }
}

private class PKFieldConverter(val idHandler: IDHandler<*>) : FieldConverter {
    override fun dbValueToParameter(value: Any?) = idHandler.createId(value as PK)

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
