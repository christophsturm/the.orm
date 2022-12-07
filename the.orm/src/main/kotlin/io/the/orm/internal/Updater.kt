package io.the.orm.internal

import io.the.orm.RepositoryException
import io.the.orm.dbio.DBConnection
import io.the.orm.internal.classinfo.ClassInfo
import kotlin.reflect.KProperty1

internal class Updater<T : Any>(
    private val idHandler: IDHandler<T>,
    private val idProperty: KProperty1<T, Any>,
    classInfo: ClassInfo<T>
) {
    private val fieldsWithoutId = classInfo.localFieldInfo.filter { it.dbFieldName != "id" }
    private val types: List<Class<*>> = listOf(Long::class.java)/*PK*/ + fieldsWithoutId.map { it.type }
    private val updateStatementString =
        run {
            val propertiesString =
                fieldsWithoutId.withIndex().joinToString { (index, value) -> "${value.dbFieldName}=$${index + 2}" }

            @Suppress("SqlResolve")
            "UPDATE ${classInfo.table.name} set $propertiesString where id=$1"
        }

    suspend fun update(connection: DBConnection, instance: T) {
        val values = fieldsWithoutId.asSequence().map { it.valueForDb(instance) }

        val id = idHandler.getId(idProperty.call(instance))
        val statement =
            connection.createStatement(updateStatementString)

        val rowsUpdated = statement.execute(types, listOf(id) + values).rowsUpdated()
        if (rowsUpdated != 1L) throw RepositoryException("rowsUpdated was $rowsUpdated instead of 1")
    }
}
