package io.the.orm.internal

import io.r2dbc.spi.R2dbcDataIntegrityViolationException
import io.the.orm.dbio.ConnectionProvider
import io.the.orm.internal.classinfo.ClassInfo
import io.vertx.pgclient.PgException

internal interface Inserter<T : Any> {
    suspend fun create(
        connectionProvider: ConnectionProvider,
        instance: EntityWrapper
    ): EntityWrapper
}

internal class SimpleInserter<T : Any>(
    private val exceptionInspector: ExceptionInspector<T>,
    classInfo: ClassInfo<T>
) : Inserter<T> {
    private val fieldsWithoutId = classInfo.localFields.filter { it.dbFieldName != "id" }
    private val types = fieldsWithoutId.map { it.type }

    private val insertStatementString = run {
        val fieldPlaceHolders = (1..fieldsWithoutId.size).joinToString { idx -> "$$idx" }
        val fields = fieldsWithoutId.joinToString { it.dbFieldName }
        "INSERT INTO ${classInfo.table.name}($fields) values ($fieldPlaceHolders)"
    }

    override suspend fun create(
        connectionProvider: ConnectionProvider,
        instance: EntityWrapper
    ): EntityWrapper {
        return connectionProvider.withConnection { connection ->
            try {
                val values = fieldsWithoutId.map { it.valueForDb(instance) }
                val statement = connection.createInsertStatement(insertStatementString)

                val id = statement.execute(types, values).getId()

                instance.withId(id)
            } catch (e: R2dbcDataIntegrityViolationException) {
                throw exceptionInspector.r2dbcDataIntegrityViolationException(e, instance)
            } catch (e: PgException) {
                throw exceptionInspector.pgException(e, instance)
            }
        }
    }
}
