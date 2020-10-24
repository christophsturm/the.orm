package r2dbcfun

import io.r2dbc.spi.Connection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.single
import r2dbcfun.internal.IDHandler
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

public interface PK {
    public val id: Long
}

public class R2dbcRepo<T : Any>(
    kClass: KClass<T>
) {
    public companion object {
        /**
         * creates a Repo for the entity <T>
         */
        public inline fun <reified T : Any> create(): R2dbcRepo<T> =
            R2dbcRepo(T::class)
    }

    private val properties = kClass.declaredMemberProperties.associateBy({ it.name }, { it })
    private val propertyReaders = properties.filter { it.key != "id" }.values.map { PropertyReader(it) }

    private val tableName = "${kClass.simpleName!!.toSnakeCase().toLowerCase()}s"

    private val idHandler = IDHandler(kClass)


    @Suppress("UNCHECKED_CAST")
    private val idProperty = properties["id"] as KProperty1<T, Any>

    private val inserter = Inserter(tableName, propertyReaders, idHandler)

    private val updater = Updater(tableName, propertyReaders, idHandler, idProperty)

    private val finder = Finder(tableName, idHandler, kClass, ClassInfo(kClass))

    /**
     * creates a new record in the database.
     * @param instance the instance that will be used to set the fields of the newly created record
     * @return a copy of the instance with an assigned id field.
     */
    public suspend fun create(connection: Connection, instance: T): T = inserter.create(connection, instance)

    /**
     * updates a record in the database.
     * @param instance the instance that will be used to update the record
     */
    public suspend fun update(connection: Connection, instance: T) {
        updater.update(connection, instance)
    }

    /**
     * loads an object from the database
     * @param id the primary key of the object to load
     */
    public suspend fun findById(connection: Connection, id: PK): T {
        return try {
            findBy(connection, idProperty, id.id).single()
        } catch (e: NoSuchElementException) {
            throw NotFoundException("No $tableName found for id ${id.id}")
        }
    }

    /**
     * finds all objects in the database where property matches propertyValue
     * @param property the property to filter by
     * @param propertyValue the value of
     */
    public suspend fun <V : Any> findBy(connection: Connection, property: KProperty1<T, V>, propertyValue: V): Flow<T> =
        finder.findBy(connection, property, propertyValue)

}

