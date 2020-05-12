package r2dbcfun

import io.r2dbc.spi.Clob
import io.r2dbc.spi.Connection
import io.r2dbc.spi.Statement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import r2dbcfun.internal.IdAssigner
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties

class R2dbcRepo<T : Any>(private val connection: Connection, kClass: KClass<out T>) {
    companion object {
        inline fun <reified T : Any> create(connection: Connection) = R2dbcRepo(connection, T::class)
    }

    private val propertyForName = kClass.declaredMemberProperties.associateBy({ it.name }, { it })
    private val propertiesExceptId = propertyForName.filter { it.key != "id" }.values
    private val snakeCaseForProperty = kClass.declaredMemberProperties.associateBy({ it }, { it.name.toSnakeCase() })

    private val tableName = "${kClass.simpleName!!.toLowerCase()}s"

    private fun makeUpdateString(): String {
        val propertiesWithoutId = propertyForName.keys.filter { it != "id" }
        val propertiesString = propertiesWithoutId.withIndex()
            .joinToString { indexedProperty -> "${indexedProperty.value.toSnakeCase()}=$${indexedProperty.index + 2}" }

        @Suppress("SqlResolve")
        return "UPDATE $tableName set $propertiesString where id=$1"
    }
    private val updateStatementString = makeUpdateString()

    private fun makeInsertStatementString(): String {
        val fieldNames = propertiesExceptId.joinToString { it.name.toSnakeCase() }
        val fieldPlaceHolders = (1..propertiesExceptId.size).joinToString { idx -> "$$idx" }
        return "INSERT INTO $tableName($fieldNames) values ($fieldPlaceHolders)"
    }
    private val insertStatementString = makeInsertStatementString()

    private val idAssigner = IdAssigner(kClass)
    private val constructor = kClass.constructors.singleOrNull { it.visibility == KVisibility.PUBLIC }
        ?: throw RuntimeException("No public constructor found for ${kClass.simpleName}")

    private val snakeCaseStringForConstructorParameter =
        constructor.parameters.associateBy({ it }, { it.name!!.toSnakeCase() })

    @Suppress("SqlResolve")
    private val selectString =
        "select ${constructor.parameters.joinToString { it.name!!.toSnakeCase() }} from $tableName where "

    @Suppress("UNCHECKED_CAST")
    private val idProperty = propertyForName["id"] as KProperty1<T, Any>


    suspend fun create(instance: T): T {
        val statement = propertiesExceptId.foldIndexed(
            connection.createStatement(insertStatementString)
        )
        { idx, statement, property ->
            bindValueOrNull(property, instance, statement, idx)
        }
        val id = try {
            statement.executeInsert()
        } catch (e: Exception) {
            throw R2dbcRepoException("error executing insert: $insertStatementString", e)
        }

        return idAssigner.assignId(instance, id)
    }

    suspend fun update(instance: T) {
        val statement = propertiesExceptId.foldIndexed(
            connection.createStatement(updateStatementString)
                .bind(0, idAssigner.getId(idProperty.call(instance)))
        ) { idx, statement, entry ->
            bindValueOrNull(entry, instance, statement, idx + 1)
        }
        val rowsUpdated = statement.execute().awaitSingle().rowsUpdated.awaitSingle()
        if (rowsUpdated != 1)
            throw R2dbcRepoException("rowsUpdated was $rowsUpdated instead of 1")

    }

    private fun bindValueOrNull(
        entry: KProperty1<out T, *>,
        instance: T,
        statement: Statement,
        index: Int
    ): Statement {
        val value = entry.call(instance)
        return try {
            if (value == null)
                statement.bindNull(index, (entry.returnType.classifier as KClass<*>).java)
            else
                statement.bind(index, value)
        } catch (e: java.lang.IllegalArgumentException) {
            throw R2dbcRepoException(
                "error binding value $value to field $entry with index $index",
                e
            )
        }
    }


    suspend fun findById(id: Long): T {
        return try {
            findBy(idProperty, id).single()
        } catch (e: NoSuchElementException) {
            throw NotFoundException("No $tableName found for id $id")
        }
    }

    suspend fun <V> findBy(property: KProperty1<T, V>, propertyValue: V): Flow<T> {
        val query = selectString + snakeCaseForProperty[property] + "=$1"
        val result = try {
            connection.createStatement(query).bind("$1", propertyValue).execute()
                .awaitSingle()
        } catch (e: Exception) {
            throw R2dbcRepoException("error executing select: $query", e)
        }
        val parameters = result.map { row, _ ->
            snakeCaseStringForConstructorParameter.mapValues { entry ->
                row.get(entry.value)
            }
        }.asFlow()
        return parameters.map {
            val resolvedParameters: Map<KParameter, Any> = it.mapValues { entry ->
                val resolvedValue = when (val value = entry.value) {
                    is Clob -> {
                        val sb = StringBuilder()
                        value.stream().asFlow().collect { chunk ->
                            @Suppress("BlockingMethodInNonBlockingContext")
                            sb.append(chunk)
                        }
                        value.discard()
                        sb.toString()
                    }
                    else -> value
                }
                if (entry.key.name == "id")
                    idAssigner.createId(resolvedValue as Long)
                else
                    resolvedValue
            }
            try {
                constructor.callBy(resolvedParameters)
            } catch (e: IllegalArgumentException) {
                throw R2dbcRepoException("error invoking constructor for $tableName. parameters:$resolvedParameters", e)
            }
        }
    }
}
