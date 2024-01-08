package io.the.orm.internal

import io.the.orm.OrmException
import io.the.orm.dbio.DBConnection
import io.the.orm.internal.classinfo.EntityInfo
import kotlin.reflect.KProperty1

internal class Updater<T : Any>(private val idProperty: KProperty1<T, Any>, classInfo: EntityInfo) {
    private val fieldsWithoutId = classInfo.localFields.filter { it.dbFieldName != "id" }
    private val types: List<Class<*>> =
        listOf(Long::class.java) /*PK*/ + fieldsWithoutId.map { it.type }
    private val updateStatementString = run {
        val propertiesString =
            fieldsWithoutId.withIndex().joinToString { (index, value) ->
                "${value.dbFieldName}=$${index + 2}"
            }

        @Suppress("SqlResolve") "UPDATE ${classInfo.table.name} set $propertiesString where id=$1"
    }

    suspend fun update(connection: DBConnection, instance: EntityWrapper) {
        val values = fieldsWithoutId.asSequence().map { it.valueForDb(instance) }

        val id = idProperty.call(instance.entity)
        val statement = connection.createStatement(updateStatementString)

        val rowsUpdated = statement.execute(types, listOf(id) + values).rowsUpdated()
        if (rowsUpdated != 1L) throw OrmException("rowsUpdated was $rowsUpdated instead of 1")
    }
}
