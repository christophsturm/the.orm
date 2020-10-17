package r2dbcfun

import io.r2dbc.spi.Connection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.single
import r2dbcfun.internal.IDHandler
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor

public interface PK {
    public val id: Long
}

public class R2dbcRepo<T : Any, PKClass : PK>(
    connection: Connection,
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

    private val tableName = "${kClass.simpleName!!.toLowerCase()}s"


    private val idAssigner = IDHandler(kClass, pkClass)
    private val constr: KFunction<T> = kClass.primaryConstructor
        ?: throw RuntimeException("No primary constructor found for ${kClass.simpleName}")


    @Suppress("UNCHECKED_CAST")
    private val idProperty = properties["id"] as KProperty1<T, Any>

    private val inserter = Inserter(tableName, connection, propertiesExceptId, idAssigner)

    private val updater = Updater(tableName, connection, propertiesExceptId, idAssigner, idProperty)

    private val finder = Finder(tableName, connection, idAssigner, constr, kClass)

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
    public suspend fun <V : Any> findBy(property: KProperty1<T, V>, propertyValue: V): Flow<T> =
        finder.findBy(property, propertyValue)


}

