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
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

internal class Finder<T : Any>(
    private val table: String,
    private val idHandler: IDHandler<T>,
    kClass: KClass<T>,
    private val classInfo: ClassInfo<T>
) {
    @Suppress("SqlResolve")
    private val selectStringPrefix =
        "select ${classInfo.fieldInfo.joinToString { it.snakeCaseName }} from $table where "

    private val snakeCaseForProperty =
        kClass.declaredMemberProperties.associateBy({ it }, { it.name.toSnakeCase() })

    suspend fun <V : Any> findBy(connection: Connection, property: KProperty1<T, V>, propertyValue: V): Flow<T> {
        val suffix = snakeCaseForProperty[property] + "=$1"
        val properties = listOf(propertyValue)
        return findBy(connection, suffix, properties)
    }

    suspend fun <V : Any> findBy(
        connection: Connection,
        query: String,
        properties: List<V>
    ): Flow<T> {
        val query = selectStringPrefix + query
        val statement = try {
            properties.foldIndexed(connection.createStatement(query)) { idx, statement, property ->
                statement.bind(
                    idx,
                    property
                )
            }
        } catch (e: Exception) {
            throw R2dbcRepoException("error creating statement", e)
        }
        val queryResult = try {
            statement.execute()
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
                    is Clob -> resolveClob(result)
                    else -> result
                }
                val value = if (fieldInfo.snakeCaseName == "id")
                    idHandler.createId(resolvedValue as Long)
                else {
                    fieldInfo.fieldConverter.valueToConstructorParameter(resolvedValue)
                }
                Pair(fieldInfo.constructorParameter, value)
            }
            try {
                classInfo.constructor.callBy(resolvedParameters)
            } catch (e: IllegalArgumentException) {
                throw R2dbcRepoException(
                    "error invoking constructor for $table. parameters:$resolvedParameters",
                    e
                )
            }
        }
    }

    private suspend fun resolveClob(result: Clob): String {
        val sb = StringBuilder()
        result.stream().asFlow().collect { chunk ->
            @Suppress("BlockingMethodInNonBlockingContext")
            sb.append(chunk)
        }
        result.discard()
        return sb.toString()
    }

}

