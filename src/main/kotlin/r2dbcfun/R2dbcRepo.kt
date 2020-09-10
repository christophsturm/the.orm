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
import r2dbcfun.internal.IDHandler
import java.lang.Enum.valueOf
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaType

public interface PK {
    public val id: Long
}

public class R2dbcRepo<T : Any, PKClass : PK>(
    private val connection: Connection,
    kClass: KClass<T>,
    pkClass: KClass<PKClass>
) {
    public companion object {
        /**
         * creates a Repo for <T> and Primary Key <PKClass>
         */
        public inline fun <reified T : Any, reified PKClass : PK> create(connection: Connection): R2dbcRepo<T, PKClass> =
            R2dbcRepo(connection, T::class, PKClass::class)
    }

    private val properties = kClass.declaredMemberProperties.associateBy({ it.name }, { it })
    private val propertiesExceptId = ArrayList(properties.filter { it.key != "id" }.values)
    private val snakeCaseForProperty = kClass.declaredMemberProperties.associateBy({ it }, { it.name.toSnakeCase() })

    private val tableName = "${kClass.simpleName!!.toLowerCase()}s"



    private val idAssigner = IDHandler(kClass, pkClass)
    private val constructor = kClass.primaryConstructor
        ?: throw RuntimeException("No primary constructor found for ${kClass.simpleName}")

    private val snakeCaseStringForConstructorParameter =
        constructor.parameters.associateBy({ it }, { it.name!!.toSnakeCase() })

    @Suppress("SqlResolve")
    private val selectString =
        "select ${constructor.parameters.joinToString { it.name!!.toSnakeCase() }} from $tableName where "

    @Suppress("UNCHECKED_CAST")
    private val idProperty = properties["id"] as KProperty1<T, Any>

    private class Inserter<T : Any, PKClass : PK>(
        val insertProperties: ArrayList<KProperty1<T, *>>,
        tableName: String,
        val connection: Connection,
        val idHandler: IDHandler<T, PKClass>
    ) {
        private val insertStatementString = run {
            val fieldNames = insertProperties.joinToString { it.name.toSnakeCase() }
            val fieldPlaceHolders = (1..insertProperties.size).joinToString { idx -> "$$idx" }
            "INSERT INTO $tableName($fieldNames) values ($fieldPlaceHolders)"
        }

        suspend fun create(instance: T): T {
            val statement = insertProperties.foldIndexed(
                connection.createStatement(insertStatementString)
            )
            { idx, statement, property ->
                bindValueOrNull(
                    statement,
                    idx,
                    property.call(instance),
                    property.returnType.classifier as KClass<*>,
                    property.name
                )
            }
            val id = try {
                statement.executeInsert()
            } catch (e: Exception) {
                throw R2dbcRepoException("error executing insert: $insertStatementString", e)
            }

            return idHandler.assignId(instance, id)
        }
    }

    private val inserter = Inserter(propertiesExceptId, tableName, connection, idAssigner)

    private inner class Updater {
        private val updateStatementString = run {
            val propertiesString = propertiesExceptId.withIndex()
                .joinToString { indexedProperty -> "${indexedProperty.value.name.toSnakeCase()}=$${indexedProperty.index + 2}" }

            @Suppress("SqlResolve")
            "UPDATE $tableName set $propertiesString where id=$1"
        }

        suspend fun update(instance: T) {
            val statement = propertiesExceptId.foldIndexed(
                connection.createStatement(updateStatementString)
                    .bind(0, idAssigner.getId(idProperty.call(instance)))
            ) { idx, statement, entry ->
                bindValueOrNull(
                    statement,
                    idx + 1,
                    entry.call(instance),
                    entry.returnType.classifier as KClass<*>,
                    entry.name
                )
            }
            val rowsUpdated = statement.execute().awaitSingle().rowsUpdated.awaitSingle()
            if (rowsUpdated != 1)
                throw R2dbcRepoException("rowsUpdated was $rowsUpdated instead of 1")

        }

    }

    private val updater = Updater()

    private inner class Finder {
        suspend fun <V> findBy(property: KProperty1<T, V>, propertyValue: V): Flow<T> {
            val query = selectString + snakeCaseForProperty[property] + "=$1"
            val queryResult = try {
                connection.createStatement(query).bind("$1", propertyValue).execute()
                    .awaitSingle()
            } catch (e: Exception) {
                throw R2dbcRepoException("error executing select: $query", e)
            }
            val parameters = queryResult.map { row, _ ->
                snakeCaseStringForConstructorParameter.mapValues { entry ->
                    row.get(entry.value)
                }
            }.asFlow()
            return parameters.map {
                val resolvedParameters: Map<KParameter, Any> = it.mapValues { (parameter, value) ->
                    val resolvedValue = when (value) {
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
                    if (parameter.name == "id")
                        idAssigner.createId(resolvedValue as Long)
                    else {

                        val clazz = parameter.type.javaType as Class<*>
                        if (resolvedValue != null && clazz.isEnum) {
                            createEnumValue(clazz, resolvedValue)
                        } else
                            resolvedValue
                    }
                }
                try {
                    constructor.callBy(resolvedParameters)
                } catch (e: IllegalArgumentException) {
                    throw R2dbcRepoException(
                        "error invoking constructor for $tableName. parameters:$resolvedParameters",
                        e
                    )
                }
            }
        }

        private fun createEnumValue(clazz: Class<*>, resolvedValue: Any?) =
            (@Suppress("UPPER_BOUND_VIOLATED", "UNCHECKED_CAST")
            valueOf<Any>(clazz as Class<Any>, resolvedValue as String))
    }

    private val finder = Finder()

    /**
     * creates a new record in the database.
     * @param instance the instance that will be used to set the fields of the newly created record
     * @return a copy of the instance with an assigned id field.
     */
    public suspend fun create(instance: T): T = inserter.create(instance)

    /**
     * updates a record in the database.
     * @param instance the instance that will be used to update the record
     */
    public suspend fun update(instance: T) {
        updater.update(instance)
    }

    /**
     * loads an object from the database
     * @param id the primary key of the object to load
     */
    public suspend fun findById(id: PK): T = try {
        findBy(idProperty, id.id).single()
    } catch (e: NoSuchElementException) {
        throw NotFoundException("No $tableName found for id ${id.id}")
    }

    /**
     * finds all objects in the database where property matches propertyValue
     * @param property the property to filter by
     * @param propertyValue the value of
     */
    public suspend fun <V> findBy(property: KProperty1<T, V>, propertyValue: V): Flow<T> =
        finder.findBy(property, propertyValue)



}

private fun bindValueOrNull(
    statement: Statement,
    index: Int,
    value: Any?,
    kClass: KClass<*>,
    fieldName: String
): Statement {
    return try {
        if (value == null) {
            val clazz = if (kClass.isSubclassOf(Enum::class)) String::class.java else kClass.java
            statement.bindNull(index, clazz)
        } else {

            statement.bind(index, if (value::class.isSubclassOf(Enum::class)) value.toString() else value)
        }
    } catch (e: java.lang.IllegalArgumentException) {
        throw R2dbcRepoException(
            "error binding value $value to field $fieldName with index $index",
            e
        )
    }
}
