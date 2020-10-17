package r2dbcfun

import io.r2dbc.spi.Clob
import io.r2dbc.spi.Connection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import r2dbcfun.internal.IDHandler
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaType

internal class Finder<T : Any, PKClass : PK>(
    private val table: String,
    private val connection: Connection,
    private val idHandler: IDHandler<T, PKClass>,
    private val constructor: KFunction<T>,
    kClass: KClass<T>
) {
    @Suppress("SqlResolve")
    private val selectStringPrefix =
        "select ${constructor.parameters.joinToString { it.name!!.toSnakeCase() }} from $table where "

    private val snakeCaseForProperty =
        kClass.declaredMemberProperties.associateBy({ it }, { it.name.toSnakeCase() })

    private val classInfo = ClassInfo(constructor)

    suspend fun <V : Any> findBy(property: KProperty1<T, V>, propertyValue: V): Flow<T> {
        val query = selectStringPrefix + snakeCaseForProperty[property] + "=$1"
        val queryResult = try {
            connection.createStatement(query).bind("$1", propertyValue).execute()
                .awaitSingle()
        } catch (e: Exception) {
            throw R2dbcRepoException("error executing select: $query", e)
        }

        data class ResultPair(val fieldInfo: ClassInfo.FieldInfo, val result: Any?)

        val parameters: Flow<List<ResultPair>> = queryResult.map { row, _ ->
            classInfo.fieldInfo.map { entry ->
                ResultPair(entry, row.get(entry.snakeCaseName))
            }
        }.asFlow()
        return parameters.map { values ->
            val resolvedParameters = values.associateTo(HashMap()) { (fieldInfo, result) ->
                val resolvedValue = when (result) {
                    is Clob -> {
                        val sb = StringBuilder()
                        result.stream().asFlow().collect { chunk ->
                            @Suppress("BlockingMethodInNonBlockingContext")
                            sb.append(chunk)
                        }
                        result.discard()
                        sb.toString()
                    }
                    else -> result
                }
                val value = if (fieldInfo.snakeCaseName == "id")
                    idHandler.createId(resolvedValue as Long)
                else {
                    val clazz = fieldInfo.constructorParameter.type.javaType as Class<*>
                    if (resolvedValue != null && clazz.isEnum) {
                        createEnumValue(clazz, resolvedValue)
                    } else
                        resolvedValue
                }
                Pair(fieldInfo.constructorParameter, value)
            }
            try {
                constructor.callBy(resolvedParameters)
            } catch (e: IllegalArgumentException) {
                throw R2dbcRepoException(
                    "error invoking constructor for $table. parameters:$resolvedParameters",
                    e
                )
            }
        }
    }


    private fun createEnumValue(clazz: Class<*>, resolvedValue: Any?) =
        @Suppress("UPPER_BOUND_VIOLATED", "UNCHECKED_CAST", "RemoveExplicitTypeArguments")
        (java.lang.Enum.valueOf<Any>(clazz as Class<Any>, resolvedValue as String))
}

internal class ClassInfo<T>(constructor: KFunction<T>) {
    data class FieldInfo(val constructorParameter: KParameter, val snakeCaseName: String) {
        constructor(parameter: KParameter) : this(parameter, parameter.name!!.toSnakeCase())
    }

    val fieldInfo =
        constructor.parameters.map { FieldInfo(it) }


}
