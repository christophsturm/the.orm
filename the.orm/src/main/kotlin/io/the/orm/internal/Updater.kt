package io.the.orm.internal

import io.the.orm.RepositoryException
import io.the.orm.dbio.DBConnection
import kotlin.reflect.KProperty1

internal class Updater<T : Any>(
    table: Table,
    private val idHandler: IDHandler<T>,
    private val idProperty: KProperty1<T, Any>,
    classInfo: ClassInfo<T>
) {
    private val fieldsWithoutId = classInfo.fieldInfo.filter { it.dbFieldName != "id" }
    private val types: List<Class<*>> = listOf(Long::class.java)/*PK*/ + fieldsWithoutId.map { it.type }
    private val updateStatementString =
        run {
            val propertiesString =
                fieldsWithoutId.withIndex().joinToString { (index, value) -> "${value.dbFieldName}=$${index + 2}" }

            @Suppress("SqlResolve") "UPDATE ${table.name} set $propertiesString where id=$1"
        }

    suspend fun update(connection: DBConnection, instance: T) {
        val values = fieldsWithoutId.asSequence().map { it.value(instance) }

        val id = idHandler.getId(idProperty.call(instance))
        val statement =
            connection.createStatement(updateStatementString)

        val rowsUpdated = statement.execute(types, sequenceOf(sequenceOf(id), values).flatten()).rowsUpdated()
        if (rowsUpdated != 1) throw RepositoryException("rowsUpdated was $rowsUpdated instead of 1")
    }
}
