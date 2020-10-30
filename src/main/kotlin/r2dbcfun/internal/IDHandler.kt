package r2dbcfun.internal

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import r2dbcfun.R2dbcRepoException

internal class IDHandler<T : Any>(kClass: KClass<out T>) {
    @Suppress("UNCHECKED_CAST")
    private val copyFunction: KFunction<T> = kClass.memberFunctions.single { it.name == "copy" } as
        KFunction<T>
    private val idParameter = copyFunction.parameters.single { it.name == "id" }
    private val instanceParameter = copyFunction.instanceParameter!!
    private val pkConstructor: KFunction<Any>?
    private val pkIdGetter: KProperty1.Getter<out Any, Any?>?

    init {
        val pkClass = idParameter.type.classifier as KClass<*>
        if (pkClass != Long::class) {
            pkConstructor = pkClass.primaryConstructor!!
            val parameters = pkConstructor.parameters
            if (parameters.singleOrNull()?.type?.classifier as? KClass<*> != Long::class)
                throw R2dbcRepoException("PK classes must have a single field of type long")
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

    fun createId(id: Long): Any = pkConstructor?.call(id) ?: id
    fun getId(id: Any): Long = (pkIdGetter?.call(id) ?: id) as Long
}
