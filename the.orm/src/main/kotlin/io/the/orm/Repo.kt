package io.the.orm

import io.the.orm.dbio.ConnectionProvider
import io.the.orm.internal.ExceptionInspector
import io.the.orm.internal.HasManyInserter
import io.the.orm.internal.Inserter
import io.the.orm.internal.SimpleInserter
import io.the.orm.internal.Updater
import io.the.orm.internal.classinfo.ClassInfo
import io.the.orm.mapper.DefaultResultMapper
import io.the.orm.mapper.RelationFetchingEntityCreator
import io.the.orm.mapper.RelationFetchingResultMapper
import io.the.orm.mapper.ResultResolver
import io.the.orm.mapper.StreamingEntityCreator
import io.the.orm.query.Conditions.isEqualToCondition
import io.the.orm.query.Query
import io.the.orm.query.QueryFactory
import io.the.orm.query.isIn
import io.the.orm.relations.Relation
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

typealias PKType = Long

internal val pKClass = Long::class

interface Repo<Entity : Any> {
    companion object {
        /** creates a Repo for the entity <T> */
        inline fun <reified T : Any> create(): Repo<T> = RepoImpl(T::class)
    }

    val queryFactory: QueryFactory<Entity>

    /**
     * creates a new record in the database.
     *
     * @param instance the instance that will be used to set the fields of the newly created record
     * @return a copy of the instance with an assigned id field.
     */
    suspend fun create(connectionProvider: ConnectionProvider, instance: Entity): Entity

    /**
     * updates a record in the database.
     *
     * @param instance the instance that will be used to update the record
     */
    suspend fun update(connectionProvider: ConnectionProvider, instance: Entity)

    /**
     * loads an object from the database
     *
     * @param id the primary key of the object to load
     */
    suspend fun findById(
        connectionProvider: ConnectionProvider,
        id: PKType,
        fetchRelations: Set<KProperty1<*, Relation>> = setOf()
    ): Entity

    /**
     * loads objects by id
     *
     * @param ids the primary key of the objects to load
     */
    suspend fun findByIds(
        connectionProvider: ConnectionProvider,
        ids: List<PKType>,
        fetchRelations: Set<KProperty1<*, Relation>> = setOf()
    ): Map<PKType, Entity>
}

class RepoImpl<Entity : Any>
internal constructor(private val kClass: KClass<Entity>, classInfos: Map<KClass<*>, ClassInfo<*>>) :
    Repo<Entity> {
    constructor(kClass: KClass<Entity>) : this(kClass, mapOf(kClass to ClassInfo(kClass)))

    private val properties = kClass.declaredMemberProperties.associateBy({ it.name }, { it })

    @Suppress("UNCHECKED_CAST")
    private val idProperty =
        (properties["id"]
            ?: throw RepositoryException("class ${kClass.simpleName} has no field named id"))
            as KProperty1<Entity, PKType>

    @Suppress("UNCHECKED_CAST")
    internal val classInfo: ClassInfo<Entity> = classInfos[kClass] as ClassInfo<Entity>
    private val idHandler = classInfo.idHandler!!

    // this will later be upgraded to an Inserter that can handle relations if needed
    private var inserter: Inserter<Entity> =
        SimpleInserter(idHandler, ExceptionInspector(classInfo.table, kClass), classInfo)

    private val updater = Updater(idProperty, classInfo)

    override val queryFactory: QueryFactory<Entity> =
        QueryFactory(
            DefaultResultMapper(ResultResolver(classInfo), StreamingEntityCreator(classInfo)),
            this,
            idHandler,
            idProperty,
            classInfo
        )

    /**
     * the repo is first created as a repo that can not fetch relations when all repos are created
     * they are upgraded to repos that can fetch relations
     */
    fun afterInit() {
        if (classInfo.hasHasManyRelations) {
            val simpleInserter = inserter
            val hasManyRepos = classInfo.hasManyRelations.map { it.repo }
            val hasManyFieldInfos =
                classInfo.hasManyRelations.map { fieldInfo ->
                    val hasManyClassInfo = fieldInfo.classInfo
                    hasManyClassInfo.belongsToRelations.singleOrNull { it.relatedClass == kClass }
                        ?: throw RepositoryException(
                            "BelongsTo field for HasMany relation ${classInfo.name}.${fieldInfo.field.name}" +
                                " not found in ${fieldInfo.classInfo.name}." +
                                " Currently you need to declare both sides of the relation"
                        )
                }
            inserter = HasManyInserter(simpleInserter, classInfo, hasManyRepos, hasManyFieldInfos)
        }
        if (classInfo.hasHasManyRelations || classInfo.hasBelongsToRelations) {
            val hasManyQueries: List<Query<*>> =
                classInfo.hasManyRelations.map {
                    it.repo.queryFactory.createQuery(it.dbFieldName + "=ANY(?)")
                }
            queryFactory.resultMapper =
                RelationFetchingResultMapper(
                    ResultResolver(classInfo),
                    RelationFetchingEntityCreator(
                        classInfo.belongsToRelations.map { it.repo },
                        StreamingEntityCreator(classInfo),
                        classInfo,
                        hasManyQueries
                    )
                )
        }
    }

    /**
     * creates a new record in the database.
     *
     * @param instance the instance that will be used to set the fields of the newly created record
     * @return a copy of the instance with an assigned id field.
     */
    override suspend fun create(connectionProvider: ConnectionProvider, instance: Entity): Entity =
        inserter.create(connectionProvider, instance)

    /**
     * updates a record in the database.
     *
     * @param instance the instance that will be used to update the record
     */
    override suspend fun update(connectionProvider: ConnectionProvider, instance: Entity) {
        connectionProvider.withConnection { connection -> updater.update(connection, instance) }
    }

    private val byIdQuery: QueryFactory<Entity>.OneParameterQuery<PKType> =
        queryFactory.createQuery(isEqualToCondition(idProperty))
    private val byIdsQuery: QueryFactory<Entity>.OneParameterQuery<Array<PKType>> by lazy {
        queryFactory.createQuery(idProperty.isIn())
    }

    /**
     * loads an object from the database
     *
     * @param id the primary key of the object to load
     */
    override suspend fun findById(
        connectionProvider: ConnectionProvider,
        id: PKType,
        fetchRelations: Set<KProperty1<*, Relation>>
    ): Entity {
        return try {
            byIdQuery.with(id).findSingle(connectionProvider, fetchRelations)
        } catch (e: NoSuchElementException) {
            throw NotFoundException("No ${classInfo.name} found for id $id")
        }
    }

    override suspend fun findByIds(
        connectionProvider: ConnectionProvider,
        ids: List<PKType>,
        fetchRelations: Set<KProperty1<*, Relation>>
    ): Map<PKType, Entity> {
        return byIdsQuery.with(ids.toTypedArray()).findAndTransform(
            connectionProvider,
            fetchRelations
        ) { flow ->
            val result = LinkedHashMap<PKType, Entity>(ids.size)
            flow.collect { result[idProperty(it)] = it }
            result
        }
    }

    override fun toString(): String {
        return "repo for ${kClass.simpleName}, norel=${classInfo.canBeFetchedWithoutRelations}"
    }
}
