package r2dbcfun

import io.r2dbc.spi.R2dbcDataIntegrityViolationException
import r2dbcfun.internal.ExceptionInspector
import r2dbcfun.internal.IDHandler
import r2dbcfun.r2dbc.R2dbcConnection
import r2dbcfun.util.toSnakeCase

internal class Inserter<T : Any>(
    table: String,
    private val insertProperties: List<PropertyReader<T>>,
    private val idHandler: IDHandler<T>,
    private val exceptionInspector: ExceptionInspector<T>
) {
    private val insertStatementString =
        run {
            val fieldNames = insertProperties.joinToString { it.name.toSnakeCase() }
            val fieldPlaceHolders = (1..insertProperties.size).joinToString { idx -> "$$idx" }
            "INSERT INTO $table($fieldNames) values ($fieldPlaceHolders)"
        }

    suspend fun create(connection: R2dbcConnection, instance: T): T {
        val statement =
            insertProperties.foldIndexed(connection.createStatement(insertStatementString))
            { idx, statement, property -> property.bindValue(statement, idx, instance) }
        val id =
            try {
                statement.executeInsert()
            } catch (e: R2dbcDataIntegrityViolationException) {
                throw exceptionInspector.r2dbcDataIntegrityViolationException(e, instance)
            }

        return idHandler.assignId(instance, id)
    }
}
