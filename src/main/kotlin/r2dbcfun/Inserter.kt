package r2dbcfun

import io.r2dbc.spi.Connection
import r2dbcfun.internal.IDHandler
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

internal class Inserter<T : Any, PKClass : PK>(
    table: String,
    val connection: Connection,
    val insertProperties: ArrayList<KProperty1<T, *>>,
    val idHandler: IDHandler<T, PKClass>
) {
    private val insertStatementString = run {
        val fieldNames = insertProperties.joinToString { it.name.toSnakeCase() }
        val fieldPlaceHolders = (1..insertProperties.size).joinToString { idx -> "$$idx" }
        "INSERT INTO $table($fieldNames) values ($fieldPlaceHolders)"
    }

    suspend fun create(instance: T): T {
        val statement = insertProperties.foldIndexed(
            connection.createStatement(insertStatementString)
        )
        { idx, statement, property ->
            bindValueOrNull(
                statement,
                idx,
                property.call(instance),
                property.returnType.classifier as KClass<*>,
                property.name
            )
        }
        val id = try {
            statement.executeInsert()
        } catch (e: Exception) {
            throw R2dbcRepoException("error executing insert: $insertStatementString", e)
        }

        return idHandler.assignId(instance, id)
    }
}
