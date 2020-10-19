package r2dbcfun

import io.r2dbc.spi.Connection
import kotlinx.coroutines.reactive.awaitSingle
import r2dbcfun.internal.IDHandler
import kotlin.reflect.KProperty1

internal class Updater<T : Any, PKClass : PK>(
    table: String,
    private val connection: Connection,
    private val updateProperties: ArrayList<PropertyReader<T>>,
    private val idHandler: IDHandler<T, PKClass>,
    private val idProperty: KProperty1<T, Any>
) {
    private val updateStatementString = run {
        val propertiesString = updateProperties.withIndex()
            .joinToString { indexedProperty -> "${indexedProperty.value.name.toSnakeCase()}=$${indexedProperty.index + 2}" }

        @Suppress("SqlResolve")
        "UPDATE $table set $propertiesString where id=$1"
    }

    suspend fun update(instance: T) {
        val statement = updateProperties.foldIndexed(
            connection.createStatement(updateStatementString)
                .bind(0, idHandler.getId(idProperty.call(instance)))
        ) { idx, statement, entry ->
            entry.bindValue(statement, idx + 1, instance)
        }
        val rowsUpdated = statement.execute().awaitSingle().rowsUpdated.awaitSingle()
        if (rowsUpdated != 1)
            throw R2dbcRepoException("rowsUpdated was $rowsUpdated instead of 1")

    }

}
