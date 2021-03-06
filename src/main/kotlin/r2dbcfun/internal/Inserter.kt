package r2dbcfun.internal

import r2dbcfun.PropertyReader
import r2dbcfun.dbio.DBConnection
import r2dbcfun.util.toSnakeCase

internal class Inserter<T : Any>(
    table: String,
    private val insertProperties: List<PropertyReader<T>>,
    private val idHandler: IDHandler<T>
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
                statement.execute(types, values).getId()

        return idHandler.assignId(instance, id)
    }
}
