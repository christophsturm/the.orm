package r2dbcfun

import io.r2dbc.spi.Clob
import io.r2dbc.spi.Connection
import io.r2dbc.spi.Statement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import r2dbcfun.internal.IDHandler

internal class Finder<T : Any>(
    private val table: String,
    private val idHandler: IDHandler<T>,
    private val classInfo: ClassInfo<T>
) {

    internal suspend fun findBy(
        connection: Connection,
        sql: String,
        parameterValues: Sequence<Any>
    ): Flow<T> {
        val statement = createStatement(parameterValues, connection, sql)
        val queryResult =
            try {
                statement.execute().awaitSingle()
            } catch (e: Exception) {
                throw RepositoryException("error executing select: $sql", e)
            }

        data class ResultPair(val fieldInfo: ClassInfo.FieldInfo, val result: Any?)

        val parameters: Flow<List<ResultPair>> =
            queryResult
                .map { row, _ ->
                    classInfo.fieldInfo
                        .map { entry -> ResultPair(entry, row.get(entry.snakeCaseName)) }
                }
                .asFlow()
        return parameters.map { values ->
            val resolvedParameters =
                values.associateTo(HashMap()) { (fieldInfo, result) ->
                    val resolvedValue = when (result) {
                        is Clob -> resolveClob(result)
                        else -> result
                    }
                    val value =
                        if (fieldInfo.snakeCaseName == "id")
                            idHandler.createId(resolvedValue as Long) else {
                                fieldInfo.fieldConverter.valueToConstructorParameter(resolvedValue)
                            }
                    Pair(fieldInfo.constructorParameter, value)
                }
            try {
                classInfo.constructor.callBy(resolvedParameters)
            } catch (e: IllegalArgumentException) {
                throw RepositoryException(
                    "error invoking constructor for $table. parameters:$resolvedParameters",
                    e
                )
            }
        }
    }

    private suspend fun resolveClob(result: Clob): String {
        val sb = StringBuilder()
        result.stream()
            .asFlow()
            .collect { chunk -> @Suppress("BlockingMethodInNonBlockingContext") sb.append(chunk) }
        result.discard()
        return sb.toString()
    }

    companion object {
        internal fun createStatement(
            parameterValues: Sequence<Any>,
            connection: Connection,
            sql: String
        ): Statement =
            try {
                parameterValues.foldIndexed(connection.createStatement(sql))
                { idx, statement, property -> statement.bind(idx, property) }
            } catch (e: Exception) {
                throw RepositoryException("error creating statement", e)
            }
    }
}
