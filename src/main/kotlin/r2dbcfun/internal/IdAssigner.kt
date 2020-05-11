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

    init {
        val kclass = idParameter.type.classifier as KClass<*>

        if (kclass != Long::class)
            throw R2dbcRepoException("Id Column type was ${kclass}, but must be ${Long::class}")
    }

    fun assignId(instance: T, id: Long): T =
        copyFunction.callBy(mapOf(idParameter to id, instanceParameter to instance))
}
