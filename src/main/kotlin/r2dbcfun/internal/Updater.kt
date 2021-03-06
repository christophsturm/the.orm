package r2dbcfun.internal

import r2dbcfun.RepositoryException
import r2dbcfun.dbio.DBConnection
import r2dbcfun.util.toSnakeCase
import kotlin.reflect.KProperty1

internal class Updater<T : Any>(
    table: String,
    private val updateProperties: PropertiesReader<T>,
    private val idHandler: IDHandler<T>,
    private val idProperty: KProperty1<T, Any>
) {
    private val types: List<Class<*>> = listOf(Long::class.java)/*PK*/ + updateProperties.types
    private val updateStatementString =
        run {
            val propertiesString =
                updateProperties.propertyReaders.withIndex()
                    .joinToString { indexedProperty ->
                        "${indexedProperty.value.name.toSnakeCase()}=$${indexedProperty.index + 2}"
                    }

            @Suppress("SqlResolve") "UPDATE $table set $propertiesString where id=$1"
        }

    suspend fun update(connection: DBConnection, instance: T) {
        val values = updateProperties.values(instance)

        val id = idHandler.getId(idProperty.call(instance))
        val statement =
            connection.createStatement(updateStatementString)

        val rowsUpdated = statement.execute(types, sequenceOf(sequenceOf(id), values).flatten()).rowsUpdated()
        if (rowsUpdated != 1) throw RepositoryException("rowsUpdated was $rowsUpdated instead of 1")
    }
}
