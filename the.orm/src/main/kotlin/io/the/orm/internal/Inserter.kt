package io.the.orm.internal

import io.the.orm.dbio.DBConnection
import io.the.orm.internal.classinfo.ClassInfo

interface Inserter<T : Any> {
    suspend fun create(connection: DBConnection, instance: T): T
}

internal class SimpleInserter<T : Any>(
    table: Table,
    private val idHandler: IDHandler<T>,
    classInfo: ClassInfo<T>
) : Inserter<T> {
    private val fieldsWithoutId = classInfo.localFieldInfo.filter { it.dbFieldName != "id" }
    private val types = fieldsWithoutId.map { it.type }

    private val insertStatementString =
        run {
            val fieldPlaceHolders = (1..fieldsWithoutId.size).joinToString { idx -> "$$idx" }
            "INSERT INTO ${table.name}(${fieldsWithoutId.joinToString { it.dbFieldName }}) values ($fieldPlaceHolders)"
        }

    override suspend fun create(connection: DBConnection, instance: T): T {
        val values = fieldsWithoutId.asSequence().map { it.valueForDb(instance) }
        val statement = connection.createInsertStatement(insertStatementString)

        val id = statement.execute(types, values).getId()

        return idHandler.assignId(instance, id)
    }
}
