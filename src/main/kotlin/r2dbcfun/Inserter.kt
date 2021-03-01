package r2dbcfun

import io.r2dbc.spi.R2dbcDataIntegrityViolationException
import r2dbcfun.dbio.DBConnection
import r2dbcfun.internal.ExceptionInspector
import r2dbcfun.internal.IDHandler
import r2dbcfun.util.toSnakeCase

internal class Inserter<T : Any>(
    table: String,
    private val insertProperties: List<PropertyReader<T>>,
    private val idHandler: IDHandler<T>,
    private val exceptionInspector: ExceptionInspector<T>
) {
    private val types: List<Class<*>> = insertProperties.map { it.dbClass }
    private val insertStatementString =
        run {
            val fieldNames = insertProperties.joinToString { it.name.toSnakeCase() }
            val fieldPlaceHolders = (1..insertProperties.size).joinToString { idx -> "$$idx" }
            "INSERT INTO $table($fieldNames) values ($fieldPlaceHolders)"
        }

    suspend fun create(connection: DBConnection, instance: T): T {
        val values = insertProperties.asSequence().map { it.value(instance) }
        val statement = connection.createInsertStatement(insertStatementString)

        val id =
            try {
                statement.execute(types, values).getId()
            } catch (e: R2dbcDataIntegrityViolationException) {
                throw exceptionInspector.r2dbcDataIntegrityViolationException(e, instance)
            }

        return idHandler.assignId(instance, id)
    }
}
