package r2dbcfun.internal

import r2dbcfun.R2dbcRepoException
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberFunctions

internal class IdAssigner<T : Any>(kClass: KClass<out T>) {
    @Suppress("UNCHECKED_CAST")
    private val copyFunction: KFunction<T> = kClass.memberFunctions.single { it.name == "copy" } as KFunction<T>
    private val idParameter = copyFunction.parameters.single { it.name == "id" }
    private val instanceParameter = copyFunction.instanceParameter!!
    private val pkConstructor: KFunction<Any>?

    init {
        val pkClass = idParameter.type.classifier as KClass<*>
        if (pkClass != Long::class) {
            pkConstructor = pkClass.constructors.single()
            val parameters = pkConstructor.parameters
            if (parameters.singleOrNull()?.type?.classifier as? KClass<*> != Long::class)
                throw R2dbcRepoException("Id Column type was ${pkClass}, but must be ${Long::class}")
        } else
            pkConstructor = null
    }

    fun assignId(instance: T, id: Long): T {
        val idFieldValue = pkConstructor?.call(id) ?: id
        return copyFunction.callBy(mapOf(idParameter to idFieldValue, instanceParameter to instance))
    }
}
