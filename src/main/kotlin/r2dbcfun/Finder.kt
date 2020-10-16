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
    val table: String,
    val connection: Connection,
    val idHandler: IDHandler<T, PKClass>,
    val constructor: KFunction<T>,
    kClass: KClass<T>
) {
    @Suppress("SqlResolve")
    private val selectString =
        "select ${constructor.parameters.joinToString { it.name!!.toSnakeCase() }} from $table where "
    private val snakeCaseStringForConstructorParameter =
        constructor.parameters.associateBy({ it }, { it.name!!.toSnakeCase() })
    private val snakeCaseForProperty =
        kClass.declaredMemberProperties.associateBy({ it }, { it.name.toSnakeCase() })


    suspend fun <V> findBy(property: KProperty1<T, V>, propertyValue: V): Flow<T> {
        val query = selectString + snakeCaseForProperty[property] + "=$1"
        val queryResult = try {
            connection.createStatement(query).bind("$1", propertyValue).execute()
                .awaitSingle()
        } catch (e: Exception) {
            throw R2dbcRepoException("error executing select: $query", e)
        }
        val parameters = queryResult.map { row, _ ->
            snakeCaseStringForConstructorParameter.mapValues { entry ->
                row.get(entry.value)
            }
        }.asFlow()
        return parameters.map {
            val resolvedParameters: Map<KParameter, Any> = it.mapValues { (parameter, value) ->
                val resolvedValue = when (value) {
                    is Clob -> {
                        val sb = StringBuilder()
                        value.stream().asFlow().collect { chunk ->
                            @Suppress("BlockingMethodInNonBlockingContext")
                            sb.append(chunk)
                        }
                        value.discard()
                        sb.toString()
                    }
                    else -> value
                }
                if (parameter.name == "id")
                    idHandler.createId(resolvedValue as Long)
                else {

                    val clazz = parameter.type.javaType as Class<*>
                    if (resolvedValue != null && clazz.isEnum) {
                        createEnumValue(clazz, resolvedValue)
                    } else
                        resolvedValue
                }
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
        @Suppress("UPPER_BOUND_VIOLATED", "UNCHECKED_CAST")
        (java.lang.Enum.valueOf<Any>(clazz as Class<Any>, resolvedValue as String))
}
