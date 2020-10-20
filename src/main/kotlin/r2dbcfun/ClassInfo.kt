package r2dbcfun

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaType

internal class ClassInfo<T : Any>(kClass: KClass<T>) {
    companion object {
        // from the r2dbc spec: https://r2dbc.io/spec/0.8.1.RELEASE/spec/html/#datatypes
        private val supportedJavaTypes = setOf<Class<*>>(
            String::class.java,
            io.r2dbc.spi.Clob::class.java,
            Boolean::class.java,
            java.nio.ByteBuffer::class.java,
            io.r2dbc.spi.Blob::class.java,
            Int::class.java,
            Byte::class.java,
            Short::class.java,
            Long::class.java
        )

        private fun makeCreator(parameter: KParameter): InstanceCreator {

            val clazz = parameter.type.javaType as Class<*>
            return if (clazz.isEnum)
                EnumCreator(clazz)
            else {
                if (parameter.name != "id" && !supportedJavaTypes.contains(clazz))
                    throw R2dbcRepoException("type ${clazz.simpleName} not supported")
                InlineCreator()
            }
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
