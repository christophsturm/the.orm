package io.the.orm.internal.classinfo

import io.the.orm.Repo
import io.the.orm.RepoImpl
import io.the.orm.internal.IDHandler
import io.the.orm.internal.Table
import io.the.orm.relations.BelongsTo
import io.the.orm.relations.HasMany
import io.the.orm.util.toSnakeCase
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaType

@Suppress("UNCHECKED_CAST")
private fun <T : Any> Map<KClass<*>, RepoImpl<*>>.getRepo(c: KClass<T>): RepoImpl<T> = get(c) as RepoImpl<T>

internal data class ClassInfo<T : Any>(
    val kClass: KClass<T>,
    val table: Table,
    val name: String,
    val constructor: KFunction<T>,
    val idHandler: IDHandler<T>?,
    val fields: List<FieldInfo>,
) {
    /** fields that directly map to a database column, and need no relation fetching */
    val simpleFields: List<SimpleLocalFieldInfo> get() = fields.filterIsInstance<SimpleLocalFieldInfo>()

    /** local fields. simple fields + belongs to fields */
    val localFields: List<LocalFieldInfo> get() = fields.filterIsInstance<LocalFieldInfo>()

    /** fields for belongs to relations. */
    val belongsToRelations: List<BelongsToFieldInfo> get() = fields.filterIsInstance<BelongsToFieldInfo>()

    /** fields for has many relations. these are not stored in the table of this class */
    val hasManyRelations: List<RemoteFieldInfo> get() = fields.filterIsInstance<RemoteFieldInfo>()

    val canBeFetchedWithoutRelations = (belongsToRelations + hasManyRelations).all { it.canBeLazy }
    val hasBelongsToRelations = belongsToRelations.isNotEmpty()
    val hasHasManyRelations = hasManyRelations.isNotEmpty()

    internal sealed interface FieldInfo {
        /** this is used when converting database rows to instances */
        val constructorParameter: KParameter

        /** for reading */
        val property: KProperty1<*, *>

        /** convert between kotlin and db types */
        val fieldConverter: FieldConverter

        /** is the field mutable (a var)? or not (a val) */
        val mutable: Boolean

        /** how is the field called in the database */
        val dbFieldName: String

        /**
         * the type that we request from the database. Usually the same type as the field, but for relations it will be
         * the PK type
         */
        val type: Class<*>
    }

    interface LocalFieldInfo : FieldInfo {
        val name: String
        fun valueForDb(instance: Any): Any?
    }

    interface RelationFieldInfo : FieldInfo {
        val relatedClass: KClass<*>
        var repo: Repo<*>
        var classInfo: ClassInfo<*>
        val canBeLazy: Boolean // can the relation be fetched later or is it necessary to create the instance
    }

    data class RemoteFieldInfo(
        override val constructorParameter: KParameter,
        override val property: KProperty1<*, *>,
        override val fieldConverter: FieldConverter,
        override val type: Class<*>,
        override val relatedClass: KClass<*>,
        override val mutable: Boolean,

        // in this case this is the field name in the remote table
        override val dbFieldName: String
    ) : FieldInfo, RelationFieldInfo {

        override lateinit var repo: Repo<*>
        override lateinit var classInfo: ClassInfo<*>
        override val canBeLazy: Boolean
            get() = true
    }

    class BelongsToFieldInfo(
        override val constructorParameter: KParameter,
        override val property: KProperty1<*, *>,
        override val dbFieldName: String,
        override val mutable: Boolean,
        override val fieldConverter: FieldConverter,
        override val type: Class<*>,
        override val relatedClass: KClass<*>,
        override val canBeLazy: Boolean,
        override val name: String
    ) : RelationFieldInfo, LocalFieldInfo {
        override fun valueForDb(instance: Any): Any? = fieldConverter.propertyToDBValue(property.call(instance))
        override lateinit var repo: Repo<*>
        override lateinit var classInfo: ClassInfo<*>
    }

    fun values(instance: T): Sequence<Any?> {
        return localFields.asSequence().map { it.valueForDb(instance) }
    }

    fun afterInit(repos: Map<KClass<out Any>, RepoImpl<out Any>>) {
        fields.forEach {
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
            val fields: List<FieldInfo> = constructor.parameters.map { parameter ->
                val type = parameter.type
                val kc = type.classifier as KClass<*>
                val kotlinClass = when (kc) {
                    BelongsTo::class, HasMany::class -> type.arguments.single().type!!.classifier as KClass<*>
                    else -> kc
                }
                val (javaClass, lazy: Boolean) = when (val t = type.javaType) {
                    is Class<*> -> Pair(t, false)
                    is ParameterizedType -> Pair(t.actualTypeArguments.single() as Class<*>, true)
                    else -> throw RuntimeException("unsupported type: ${t.typeName}")
                }

                val fieldName = parameter.name!!.toSnakeCase()
                val property = properties[parameter.name]!!
                if (kc == HasMany::class) RemoteFieldInfo(
                    parameter, property, HasManyConverter(),
                    Long::class.java, kotlinClass, isMutable(property), table.baseName + "_id"
                )
                else if (otherClasses.contains(kotlinClass)) {
                    BelongsToFieldInfo(
                        parameter,
                        property,
                        fieldName + "_id",
                        isMutable(property),
                        BelongsToConverter(IDHandler(kotlinClass)),
                        Long::class.java,
                        kotlinClass, lazy, "$name.${property.name}"
                    )
                } else when {
                    javaClass.isEnum -> SimpleLocalFieldInfo(
                        parameter, property, fieldName,
                        EnumConverter(javaClass), String::class.java, isMutable(property), "$name.${property.name}"
                    )

                    else -> {
                        val isPK = parameter.name == "id"
                        if (isPK) {
                            SimpleLocalFieldInfo(
                                parameter,
                                property,
                                fieldName,
                                passThroughFieldConverter,
                                Long::class.java,
                                isMutable(property),
                                "$name.${property.name}"
                            )
                        } else {
                            SimpleLocalFieldInfo(
                                parameter, property, fieldName,
                                kotlinClass, javaClass, isMutable(property), "$name.${property.name}", otherClasses
                            )
                        }
                    }
                }
            }
            return ClassInfo(
                kClass,
                table,
                name!!,
                constructor,
                idHandler,
                fields
            )
        }

        private fun <T : Any> isMutable(property: KProperty1<T, *>) = property is KMutableProperty<*>
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
