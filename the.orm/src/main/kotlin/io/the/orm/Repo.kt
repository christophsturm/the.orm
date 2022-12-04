package io.the.orm

import io.the.orm.dbio.ConnectionProvider
import io.the.orm.internal.ExceptionInspector
import io.the.orm.internal.IDHandler
import io.the.orm.internal.Inserter
import io.the.orm.internal.SimpleInserter
import io.the.orm.internal.Table
import io.the.orm.internal.Updater
import io.the.orm.internal.classinfo.ClassInfo
import io.the.orm.mapper.DefaultResultMapper
import io.the.orm.mapper.RelationFetchingEntityCreator
import io.the.orm.mapper.RelationFetchingResultMapper
import io.the.orm.mapper.ResultResolver
import io.the.orm.mapper.StreamingEntityCreator
import io.the.orm.query.Conditions.isEqualToCondition
import io.the.orm.query.QueryFactory
import io.the.orm.query.isIn
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

typealias PK = Long

internal val pKClass = Long::class

interface Repo<T : Any> {
    companion object {
        /** creates a Repo for the entity <T> */
        inline fun <reified T : Any> create(): Repo<T> = RepoImpl(T::class)
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

    /**
     * loads objects by id
     *
     * @param [ids] the primary key of the objects to load
     */
    suspend fun findByIds(connectionProvider: ConnectionProvider, ids: List<PK>): Map<PK, T>
}

class RepoImpl<T : Any>(kClass: KClass<T>, otherClasses: Set<KClass<*>> = emptySet()) :
    Repo<T> {
    private val properties = kClass.declaredMemberProperties.associateBy({ it.name }, { it })

    private val table = Table(kClass)

    @Suppress("UNCHECKED_CAST")
    private val idProperty =
        (properties["id"]
            ?: throw RepositoryException("class ${kClass.simpleName} has no field named id")) as
            KProperty1<T, PK>

    private val idHandler = IDHandler(kClass)
    private val classInfo = ClassInfo(kClass, otherClasses)

    private val inserter: Inserter<T> = SimpleInserter(table, idHandler, ExceptionInspector(table, kClass), classInfo)

    private val updater = Updater(table, idHandler, idProperty, classInfo)

    override val queryFactory: QueryFactory<T> =
        QueryFactory(
            table,
            if (classInfo.hasBelongsToRelations) RelationFetchingResultMapper(
                ResultResolver(classInfo),
                RelationFetchingEntityCreator(
                    classInfo.belongsToRelations.map { RepoImpl(it.relatedClass!!, otherClasses + kClass) },
                    StreamingEntityCreator(classInfo)
                )
            )
            else DefaultResultMapper(
                ResultResolver(classInfo), StreamingEntityCreator(classInfo)
            ),
            this,
            idHandler,
            idProperty,
            classInfo
        )

    /**
     * creates a new record in the database.
     *
     * @param instance the instance that will be used to set the fields of the newly created record
     * @return a copy of the instance with an assigned id field.
     */
    override suspend fun create(connectionProvider: ConnectionProvider, instance: T): T =
        inserter.create(connectionProvider, instance)

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

    private val byIdQuery by lazy { queryFactory.createQuery(isEqualToCondition(idProperty)) }
    private val byIdsQuery: QueryFactory<T>.OneParameterQuery<Array<PK>>
        by lazy { queryFactory.createQuery(idProperty.isIn()) }

    /**
     * loads an object from the database
     *
     * @param id the primary key of the object to load
     */
    override suspend fun findById(connectionProvider: ConnectionProvider, id: PK): T {
        return try {
            byIdQuery.with(connectionProvider, id).findSingle()
        } catch (e: NoSuchElementException) {
            throw NotFoundException("No ${table.name} found for id $id")
        }
    }

    override suspend fun findByIds(connectionProvider: ConnectionProvider, ids: List<PK>): Map<PK, T> {
        return byIdsQuery.with(connectionProvider, ids.toTypedArray()).findAndTransform { flow ->
            val result = mutableMapOf<PK, T>()
            flow.collect {
                result[idProperty(it)] = it
            }
            result
        }
    }
}
