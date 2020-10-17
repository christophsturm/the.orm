package r2dbcfun

import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

internal class ClassInfo<T>(val constructor: KFunction<T>) {
    data class FieldInfo(val constructorParameter: KParameter, val snakeCaseName: String) {
        constructor(parameter: KParameter) : this(parameter, parameter.name!!.toSnakeCase())
    }

    val fieldInfo =
        constructor.parameters.map { FieldInfo(it) }


}
