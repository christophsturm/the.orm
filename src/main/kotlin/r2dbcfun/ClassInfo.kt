package r2dbcfun

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaType

internal class ClassInfo<T : Any>(kClass: KClass<T>) {
    companion object {
        private fun makeCreator(parameter: KParameter): InstanceCreator {
            val clazz = parameter.type.javaType as Class<*>
            return if (clazz.isEnum)
                EnumCreator(clazz)
            else
                InlineCreator()
        }
    }

    data class FieldInfo(
        val constructorParameter: KParameter,
        val snakeCaseName: String,
        val instanceCreator: InstanceCreator
    ) {
        constructor(parameter: KParameter) : this(parameter, parameter.name!!.toSnakeCase(), makeCreator(parameter))

    }

    val constructor: KFunction<T> = kClass.primaryConstructor
        ?: throw RuntimeException("No primary constructor found for ${kClass.simpleName}")

    val fieldInfo =
        constructor.parameters.map { FieldInfo(it) }


}

internal class EnumCreator(private val clazz: Class<*>) : InstanceCreator {
    override fun valueToConstructorParameter(value: Any?): Any? {
        if (value == null)
            return null

        @Suppress("UPPER_BOUND_VIOLATED", "UNCHECKED_CAST", "RemoveExplicitTypeArguments")
        return (java.lang.Enum.valueOf<Any>(clazz as Class<Any>, value as String))
    }
}

internal class InlineCreator : InstanceCreator {
    override fun valueToConstructorParameter(value: Any?): Any? = value
}

internal interface InstanceCreator {
    fun valueToConstructorParameter(value: Any?): Any?

}
