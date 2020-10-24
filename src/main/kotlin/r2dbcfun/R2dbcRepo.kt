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
    private val connection: Connection,
    kClass: KClass<T>
) {
    public companion object {
        /**
         * creates a Repo for the entity <T>
         */
        public inline fun <reified T : Any> create(connection: Connection): R2dbcRepo<T> =
            R2dbcRepo(connection, T::class)
    }

    private val properties = kClass.declaredMemberProperties.associateBy({ it.name }, { it })
    private val propertyReaders = properties.filter { it.key != "id" }.values.map { PropertyReader(it) }

    private val tableName = "${kClass.simpleName!!.toSnakeCase().toLowerCase()}s"

    private val idHandler = IDHandler(kClass)


    @Suppress("UNCHECKED_CAST")
    private val idProperty = properties["id"] as KProperty1<T, Any>

    private val inserter = Inserter(tableName, connection, propertyReaders, idHandler)

    private val updater = Updater(tableName, connection, propertyReaders, idHandler, idProperty)

    private val finder = Finder(tableName, idHandler, kClass, ClassInfo(kClass))

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
    public suspend fun findById(id: PK): T {
        return try {
            findBy(idProperty, id.id).single()
        } catch (e: NoSuchElementException) {
            throw NotFoundException("No $tableName found for id ${id.id}")
        }
    }

    /**
     * finds all objects in the database where property matches propertyValue
     * @param property the property to filter by
     * @param propertyValue the value of
     */
    public suspend fun <V : Any> findBy(property: KProperty1<T, V>, propertyValue: V): Flow<T> =
        finder.findBy(connection, property, propertyValue)

}

