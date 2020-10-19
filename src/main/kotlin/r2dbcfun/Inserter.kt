package r2dbcfun

import io.r2dbc.spi.Connection
import r2dbcfun.internal.IDHandler

internal class Inserter<T : Any>(
    table: String,
    private val connection: Connection,
    private val insertProperties: List<PropertyReader<T>>,
    private val idHandler: IDHandler<T>
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
            property.bindValue(statement, idx, instance)
        }
        val id = try {
            statement.executeInsert()
        } catch (e: Exception) {
            throw R2dbcRepoException("error executing insert: $insertStatementString", e)
        }

        return idHandler.assignId(instance, id)
    }
}
