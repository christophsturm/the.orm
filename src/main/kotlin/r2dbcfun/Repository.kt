package r2dbcfun

import io.r2dbc.spi.R2dbcDataIntegrityViolationException
import io.vertx.pgclient.PgException
import kotlinx.coroutines.flow.single
import r2dbcfun.dbio.ConnectionProvider
import r2dbcfun.internal.ClassInfo
import r2dbcfun.internal.ExceptionInspector
import r2dbcfun.internal.IDHandler
import r2dbcfun.internal.Inserter
import r2dbcfun.internal.Table
import r2dbcfun.internal.Updater
import r2dbcfun.query.QueryFactory
import r2dbcfun.query.QueryFactory.Companion.isEqualToCondition
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

interface PK {
    val id: Long
}

interface Repository<T : Any> {
    companion object {
        /** creates a Repo for the entity <T> */
        inline fun <reified T : Any> create(): Repository<T> = RepositoryImpl(T::class)
    }

    val queryFactory: QueryFactory<T>

    /**
     * creates a new record in the database.
     *
     * @param instance the instance that will be used to set the fields of the newly created record
     * @return a copy of the instance with an assigned id field.
     */
    suspend fun create(connectionProvider: ConnectionProvider, instance: T): T

    /**
     * updates a record in the database.
     *
     * @param instance the instance that will be used to update the record
     */
    suspend fun update(connectionProvider: ConnectionProvider, instance: T)

    /**
     * loads an object from the database
     *
     * @param id the primary key of the object to load
     */
    suspend fun findById(connectionProvider: ConnectionProvider, id: PK): T
}

class RepositoryImpl<T : Any>(kClass: KClass<T>, otherClasses: Set<KClass<*>> = emptySet()) : Repository<T> {

    private val properties = kClass.declaredMemberProperties.associateBy({ it.name }, { it })

    private val table = Table(kClass)

    @Suppress("UNCHECKED_CAST")
    private val idProperty =
        (properties["id"]
            ?: throw RepositoryException("class ${kClass.simpleName} has no field named id")) as
                KProperty1<T, Any>

    private val idHandler = IDHandler(kClass)
    private val classInfo = ClassInfo(kClass, idHandler, otherClasses)

    private val exceptionInspector = ExceptionInspector(table, kClass)

    private val inserter = Inserter(table, idHandler, classInfo)

    private val updater = Updater(table, idHandler, idProperty, classInfo)


    override val queryFactory: QueryFactory<T> =
        QueryFactory(table, kClass, ResultMapperImpl(classInfo), this, idHandler, idProperty)

    /**
     * creates a new record in the database.
     *
     * @param instance the instance that will be used to set the fields of the newly created record
     * @return a copy of the instance with an assigned id field.
     */
    override suspend fun create(connectionProvider: ConnectionProvider, instance: T): T =
        connectionProvider.withConnection { connection ->
            try {
                inserter.create(connection, instance)
            } catch (e: R2dbcDataIntegrityViolationException) {
                throw exceptionInspector.r2dbcDataIntegrityViolationException(e, instance)
            } catch (e: PgException) {
                throw exceptionInspector.pgException(e, instance)
            }
        }

    /**
     * updates a record in the database.
     *
     * @param instance the instance that will be used to update the record
     */
    override suspend fun update(connectionProvider: ConnectionProvider, instance: T) {
        connectionProvider.withConnection { connection ->
            updater.update(connection, instance)
        }
    }

    private val byIdQuery = queryFactory.createQuery(isEqualToCondition(idProperty))

    /**
     * loads an object from the database
     *
     * @param id the primary key of the object to load
     */
    override suspend fun findById(connectionProvider: ConnectionProvider, id: PK): T {
        return try {
            byIdQuery.with(connectionProvider, id.id).find().single()
        } catch (e: NoSuchElementException) {
            throw NotFoundException("No ${table.name} found for id ${id.id}")
        }
    }
}
