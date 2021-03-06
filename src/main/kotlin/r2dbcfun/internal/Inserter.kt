package r2dbcfun.internal

import r2dbcfun.dbio.DBConnection

internal class Inserter<T : Any>(
    table: Table,
    private val insertProperties: PropertiesReader<T>,
    private val idHandler: IDHandler<T>
) {
    private val insertStatementString =
        run {
            val fieldPlaceHolders = (1..insertProperties.propertyReaders.size).joinToString { idx -> "$$idx" }
            "INSERT INTO ${table.name}(${insertProperties.fieldNames}) values ($fieldPlaceHolders)"
        }

    suspend fun create(connection: DBConnection, instance: T): T {
        val values = insertProperties.values(instance)
        val statement = connection.createInsertStatement(insertStatementString)

        val id = statement.execute(insertProperties.types, values).getId()

        return idHandler.assignId(instance, id)
    }
}
