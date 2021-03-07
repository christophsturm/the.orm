package r2dbcfun.internal

import r2dbcfun.dbio.DBConnection

internal class Inserter<T : Any>(
    table: Table,
    private val idHandler: IDHandler<T>,
    classInfo: ClassInfo<T>
) {
    val fieldsWithoutId = classInfo.fieldInfo.filter { it.dbFieldName != "id" }
    val types = fieldsWithoutId.map {
        it.type
    }

    private val insertStatementString =
        run {
            val fieldPlaceHolders = (1..fieldsWithoutId.size).joinToString { idx -> "$$idx" }
            "INSERT INTO ${table.name}(${fieldsWithoutId.joinToString { it.dbFieldName }}) values ($fieldPlaceHolders)"
        }

    suspend fun create(connection: DBConnection, instance: T): T {
        val values = fieldsWithoutId.asSequence().map { it.value(instance) }
        val statement = connection.createInsertStatement(insertStatementString)

        val id = statement.execute(types, values).getId()

        return idHandler.assignId(instance, id)
    }
}
