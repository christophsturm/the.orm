package r2dbcfun

import io.r2dbc.spi.Connection
import r2dbcfun.internal.IDHandler

internal class Inserter<T : Any, PKClass : PK>(
    table: String,
    private val connection: Connection,
    private val insertProperties: ArrayList<PropertyReader<T>>,
    private val idHandler: IDHandler<T, PKClass>
) {
    private val insertStatementString = run {
        val fieldNames = insertProperties.joinToString { it.property.name.toSnakeCase() }
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
                property.property.call(instance),
                property.kClass,
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
