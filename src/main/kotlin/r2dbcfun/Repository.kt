package r2dbcfun

import io.r2dbc.spi.Connection
import kotlinx.coroutines.flow.single
import r2dbcfun.internal.ExceptionInspector
import r2dbcfun.internal.IDHandler
import r2dbcfun.query.QueryFactory
import r2dbcfun.query.QueryFactory.Companion.isEqualToCondition
import r2dbcfun.util.toSnakeCase
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

public interface PK {
    public val id: Long
}

public class Repository<T : Any>(kClass: KClass<T>) {
    public companion object {
        /** creates a Repo for the entity <T> */
        public inline fun <reified T : Any> create(): Repository<T> = Repository(T::class)
    }

    private val properties = kClass.declaredMemberProperties.associateBy({ it.name }, { it })
    private val propertyReaders =
        properties.filter { it.key != "id" }.values.map { PropertyReader(it) }

    private val tableName = "${kClass.simpleName!!.toSnakeCase().toLowerCase()}s"

    @Suppress("UNCHECKED_CAST")
    private val idProperty =
        (properties["id"]
            ?: throw RepositoryException("class ${kClass.simpleName} has no field named id")) as
                KProperty1<T, Any>

    private val idHandler = IDHandler(kClass)

    private val exceptionInspector = ExceptionInspector(tableName, kClass)

    private val inserter = Inserter(tableName, propertyReaders, idHandler, exceptionInspector)

    private val updater = Updater(tableName, propertyReaders, idHandler, idProperty)

    private val classInfo = ClassInfo(kClass)

    public val queryFactory: QueryFactory<T> =
        QueryFactory(kClass, ResultMapper(tableName, idHandler, classInfo))

    /**
     * creates a new record in the database.
     *
     * @param instance the instance that will be used to set the fields of the newly created record
     * @return a copy of the instance with an assigned id field.
     */
    public suspend fun create(connection: Connection, instance: T): T =
        inserter.create(connection, instance)

    /**
     * updates a record in the database.
     *
     * @param instance the instance that will be used to update the record
     */
    public suspend fun update(connection: Connection, instance: T) {
        updater.update(connection, instance)
    }

    private val byIdQuery = queryFactory.createQuery(isEqualToCondition(idProperty))

    /**
     * loads an object from the database
     *
     * @param id the primary key of the object to load
     */
    public suspend fun findById(connection: Connection, id: PK): T {
        return try {
            byIdQuery.with(connection, id.id).find().single()
        } catch (e: NoSuchElementException) {
            throw NotFoundException("No $tableName found for id ${id.id}")
        }
    }
}
