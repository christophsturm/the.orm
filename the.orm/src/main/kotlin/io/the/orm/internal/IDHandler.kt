package io.the.orm.internal

import io.the.orm.RepositoryException
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

internal class IDHandler<T : Any>(kClass: KClass<out T>) {
    @Suppress("UNCHECKED_CAST")
    private val copyFunction: KFunction<T> =
        (kClass.memberFunctions.singleOrNull { it.name == "copy" }
            ?: throw RepositoryException(
                "no copy function found for ${kClass.simpleName}." +
                    " Entities must be data classes"
            )) as KFunction<T>
    private val idParameter = copyFunction.parameters.single { it.name == "id" }
    private val idField = kClass.declaredMemberProperties.single { it.name == "id" }
    private val instanceParameter = copyFunction.instanceParameter!!
    private val pkConstructor: KFunction<Any>?
    private val pkIdGetter: KProperty1.Getter<out Any, Any?>?

    init {
        val pkClass = idParameter.type.classifier as KClass<*>
        if (pkClass != Long::class) {
            pkConstructor = pkClass.primaryConstructor!!
            val parameters = pkConstructor.parameters
            if (parameters.singleOrNull()?.type?.classifier as? KClass<*> != Long::class)
                throw RepositoryException("PK classes must have a single field of type long")
            pkIdGetter = pkClass.memberProperties.single().getter
        } else {
            pkConstructor = null
            pkIdGetter = null
        }
    }

    fun assignId(instance: T, id: Long): T {
        return copyFunction.callBy(
            mapOf(idParameter to createId(id), instanceParameter to instance)
        )
    }
    fun readId(instance: T): Long {
        return idField.getter.call(instance) as Long
    }

    fun createId(id: Long): Any = pkConstructor?.call(id) ?: id
    fun getId(id: Any): Long = (pkIdGetter?.call(id) ?: id) as Long
}
