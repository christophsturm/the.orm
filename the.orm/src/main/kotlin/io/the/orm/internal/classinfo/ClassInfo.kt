package io.the.orm.internal.classinfo

import io.r2dbc.spi.Blob
import io.r2dbc.spi.Clob
import io.the.orm.Repo
import io.the.orm.RepoImpl
import io.the.orm.RepositoryException
import io.the.orm.exp.relations.BelongsTo
import io.the.orm.exp.relations.HasMany
import io.the.orm.getRepo
import io.the.orm.internal.IDHandler
import io.the.orm.internal.Table
import io.the.orm.util.toSnakeCase
import io.vertx.sqlclient.data.Numeric
import java.lang.reflect.ParameterizedType
import java.math.BigDecimal
import java.nio.ByteBuffer
import java.time.LocalDate
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
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
    val table: Table,
    val name: String,
    val constructor: KFunction<T>,
    val idHandler: IDHandler<T>?,
    val allFields: List<FieldInfo>,
    /**
     * local fields. Fields that are stored in the table of this class. can be simple local fields or belongs to relations
     */
    val localFieldInfo: List<LocalFieldInfo>,
    /**
     * simple fields
     */
    val simpleFieldInfo: List<SimpleLocalFieldInfo>,
    /**
     * fields for belongs to relations.
     */
    val belongsToRelations: List<LocalRelationFieldInfo>,
    /**
     * fields for has many relations. these are not stored in the table of this class
     */
    val hasManyRelations: List<RemoteFieldInfo>
) {
    val hasBelongsToRelations = belongsToRelations.isNotEmpty()
    val hasHasManyRelations = hasManyRelations.isNotEmpty()

    sealed interface FieldInfo {
        val constructorParameter: KParameter
        val property: KProperty1<*, *>
        val fieldConverter: FieldConverter
        val mutable: Boolean
        val dbFieldName: String

        /**
         * then type that we request from the database.
         * Usually the same type as the field, but for relations it will be the PK type
         */
        val type: Class<*>
    }

    interface LocalFieldInfo : FieldInfo {
        fun valueForDb(instance: Any): Any?
    }

    interface RelationFieldInfo : FieldInfo {
        val relatedClass: KClass<*>
        var repo: Repo<*>
        var classInfo: ClassInfo<*>
    }

    data class RemoteFieldInfo(
        override val constructorParameter: KParameter,
        override val property: KProperty1<*, *>,
        override val fieldConverter: FieldConverter,
        override val type: Class<*>,
        override val relatedClass: KClass<*>,
        override val mutable: Boolean,
        override val dbFieldName: String // in this case this is the field name in the remote table
    ) : FieldInfo, RelationFieldInfo {
        override lateinit var repo: Repo<*>
        override lateinit var classInfo: ClassInfo<*>
    }

    data class SimpleLocalFieldInfo(
        override val constructorParameter: KParameter,
        override val property: KProperty1<*, *>,
        override val dbFieldName: String,
        override val fieldConverter: FieldConverter,
        override val type: Class<*>,
        override val mutable: Boolean
    ) : LocalFieldInfo {
        override fun valueForDb(instance: Any): Any? = fieldConverter.propertyToDBValue(property.call(instance))
    }

    class LocalRelationFieldInfo(
        override val constructorParameter: KParameter,
        override val property: KProperty1<*, *>,
        override val dbFieldName: String,
        override val mutable: Boolean,
        override val fieldConverter: FieldConverter,
        override val type: Class<*>,
        override val relatedClass: KClass<*>
    ) : RelationFieldInfo, LocalFieldInfo {
        override fun valueForDb(instance: Any): Any? = fieldConverter.propertyToDBValue(property.call(instance))
        override lateinit var repo: Repo<*>
        override lateinit var classInfo: ClassInfo<*>
    }

    fun values(instance: T): Sequence<Any?> {
        return localFieldInfo.asSequence().map { it.valueForDb(instance) }
    }

    fun afterInit(repos: Map<KClass<out Any>, RepoImpl<out Any>>) {
        allFields.forEach {
            if (it is RelationFieldInfo) {
                val repo = repos.getRepo(it.relatedClass)
                it.repo = repo
                it.classInfo = repo.classInfo
            }
        }
    }

    companion object {
        internal operator fun <T : Any> invoke(
            kClass: KClass<T>,
            otherClasses: Set<KClass<*>> = setOf()
        ): ClassInfo<T> {
            val table = Table(kClass)
            val properties: Map<String, KProperty1<T, *>> =
                kClass.declaredMemberProperties.associateBy({ it.name }, { it })

            val name = kClass.simpleName
            val constructor: KFunction<T> = kClass.primaryConstructor
                ?: throw RuntimeException("No primary constructor found for ${kClass.simpleName}")

            val idHandler = try {
                IDHandler(kClass)
            } catch (e: Exception) {
                null
            }
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
                    parameter, property, HasManyConverter(),
                    Long::class.java, kotlinClass, mutable(property), table.baseName + "_id"
                )
                else if (otherClasses.contains(kotlinClass)) {
                    LocalRelationFieldInfo(
                        parameter,
                        property,
                        fieldName + "_id",
                        mutable(property),
                        BelongsToConverter(IDHandler(kotlinClass)),
                        Long::class.java,
                        kotlinClass
                    )
                } else when {
                    javaClass.isEnum -> SimpleLocalFieldInfo(
                        parameter, property, fieldName,
                        EnumConverter(javaClass), String::class.java, mutable(property)
                    )

                    else -> {
                        val isPK = parameter.name == "id"
                        if (isPK) {
                            SimpleLocalFieldInfo(
                                parameter, property, fieldName,
                                passThroughFieldConverter, Long::class.java, mutable(property)
                            )
                        } else {
                            val fieldConverter = fieldConverters[kotlinClass] ?: throw RepositoryException(
                                "type ${kotlinClass.simpleName} not supported." +
                                    " class: ${kClass.simpleName}," +
                                    " otherClasses: ${otherClasses.map { it.simpleName }}"
                            )
                            SimpleLocalFieldInfo(
                                parameter, property, fieldName,
                                fieldConverter, javaClass, mutable<T>(property)
                            )
                        }
                    }
                }
            }
            val localFieldInfo = fieldInfo.filterIsInstance<LocalFieldInfo>()
            return ClassInfo(
                table,
                name!!,
                constructor,
                idHandler,
                fieldInfo,
                localFieldInfo,
                fieldInfo.filterIsInstance<SimpleLocalFieldInfo>(),
                fieldInfo.filterIsInstance<LocalRelationFieldInfo>(),
                fieldInfo.filterIsInstance<RemoteFieldInfo>()
            )
        }

        private fun <T : Any> mutable(property: KProperty1<T, *>) = property is KMutableProperty<*>
    }
}

/** converts strings from the database to enums in the mapped class */
private class EnumConverter(private val clazz: Class<*>) : FieldConverter {
    override fun dbValueToParameter(value: Any?): Any? {
        if (value == null) return null

        @Suppress(
            "UPPER_BOUND_VIOLATED", "UNCHECKED_CAST", "RemoveExplicitTypeArguments"
        )
        return (java.lang.Enum.valueOf<Any>(clazz as Class<Any>, value as String))
    }

    override fun propertyToDBValue(value: Any?): Any? {
        return value?.toString()
    }
}
