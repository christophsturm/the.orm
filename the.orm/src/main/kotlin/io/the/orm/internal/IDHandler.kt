package io.the.orm.internal

import io.the.orm.PKType
import io.the.orm.RepositoryException
import io.the.orm.mapper.friendlyString
import io.the.orm.pKClass
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
    private val idParameter = copyFunction.parameters.singleOrNull { it.name == "id" }
        ?: throw RepositoryException("class ${kClass.simpleName} has no field named id")
    private val idField = kClass.declaredMemberProperties.single { it.name == "id" }
    private val instanceParameter = copyFunction.instanceParameter!!
    private val pkConstructor: KFunction<Any>?
    private val pkIdGetter: KProperty1.Getter<out Any, Any?>?

    init {
        val pkClass = idParameter.type.classifier as KClass<*>
        if (pkClass != pKClass) {
            pkConstructor = pkClass.primaryConstructor!!
            val parameters = pkConstructor.parameters
            if (parameters.singleOrNull()?.type?.classifier as? KClass<*> != pKClass)
                throw RepositoryException(
                    "PK classes must have a single field of type ${pKClass.simpleName}." +
                        "$parameters"
                )
            pkIdGetter = pkClass.memberProperties.single().getter
        } else {
            pkConstructor = null
            pkIdGetter = null
        }
    }

    fun assignId(instance: T, id: PKType): T {
        val args = mapOf(idParameter to id, instanceParameter to instance)
        return try {
            copyFunction.callBy(args)
        } catch (e: IllegalArgumentException) {
            throw RepositoryException("Error assigning ID. args:${args.friendlyString()}")
        }
    }

    /**
     *  read the id field from an entity
     *  */
    fun readId(instance: T): PKType {
        when (val idResult = idField.getter.call(instance)) {
            is PKType -> return idResult
            null -> throw RepositoryException("id is not yet set for $instance")
            else -> throw RepositoryException("unknown pk type for $instance")
        }
    }
}
