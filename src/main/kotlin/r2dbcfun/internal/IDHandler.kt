package r2dbcfun.internal

import r2dbcfun.PK
import r2dbcfun.R2dbcRepoException
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties

internal class IDHandler<T : Any, PKType : PK>(kClass: KClass<T>, pkClass: KClass<PKType>) {
    @Suppress("UNCHECKED_CAST")
    private val copyFunction: KFunction<T> = kClass.memberFunctions.single { it.name == "copy" } as KFunction<T>
    private val idParameter = copyFunction.parameters.single { it.name == "id" }
    private val instanceParameter = copyFunction.instanceParameter!!
    private val pkConstructor: KFunction<PKType>
    private val pkIdGetter: KProperty1.Getter<out PK, Long>

    init {
        if (pkClass != idParameter.type.classifier as KClass<*>)


            if (!pkClass.isSubclassOf(PK::class))
                throw R2dbcRepoException("PK Classes must implement the PK interface")
        pkConstructor = pkClass.constructors.single()
        val parameters = pkConstructor.parameters
        if (parameters.singleOrNull()?.type?.classifier as? KClass<*> != Long::class)
            throw R2dbcRepoException("PK classes must have a single field of type long")
        @Suppress("UNCHECKED_CAST")
        pkIdGetter = pkClass.memberProperties.single().getter as KProperty1.Getter<out PK, Long>

    }

    fun assignId(instance: T, id: Long): T {
        return copyFunction.callBy(mapOf(idParameter to createId(id), instanceParameter to instance))
    }

    fun createId(id: Long): PKType = pkConstructor.call(id)
    fun getId(id: Any): Long = pkIdGetter.call(id)
}
