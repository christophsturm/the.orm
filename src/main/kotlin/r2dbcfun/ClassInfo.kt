package r2dbcfun

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor

internal class ClassInfo<T : Any>(kClass: KClass<T>) {
    data class FieldInfo(val constructorParameter: KParameter, val snakeCaseName: String) {
        constructor(parameter: KParameter) : this(parameter, parameter.name!!.toSnakeCase())
    }

    val constructor: KFunction<T> = kClass.primaryConstructor
        ?: throw RuntimeException("No primary constructor found for ${kClass.simpleName}")

    val fieldInfo =
        constructor.parameters.map { FieldInfo(it) }


}
