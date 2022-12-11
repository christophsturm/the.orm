package io.the.orm.query

import io.the.orm.PK
import io.the.orm.Repo
import io.the.orm.dbio.ConnectionProvider
import io.the.orm.dbio.DBConnection
import io.the.orm.dbio.DBResult
import io.the.orm.exp.relations.Relation
import io.the.orm.internal.IDHandler
import io.the.orm.internal.classinfo.ClassInfo
import io.the.orm.mapper.ResultMapper
import io.the.orm.util.toIndexedPlaceholders
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import kotlin.reflect.KProperty1

interface Query<T : Any> {
    fun with(vararg parameter: Any): QueryFactory<T>.QueryWithParameters
}

class QueryFactory<Entity : Any> internal constructor(
    internal var resultMapper: ResultMapper<Entity>,
    private val repository: Repo<Entity>,
    private val idHandler: IDHandler<Entity>,
    private val idProperty: KProperty1<Entity, Any>,
    classInfo: ClassInfo<Entity>
) {

    private val dbFieldNameForProperty =
        classInfo.localFieldInfo.associateBy({ it.property }, { it.dbFieldName })

    private val selectPrefix =
        "select ${classInfo.localFieldInfo.joinToString { it.dbFieldName }} from ${classInfo.table.name} where "
    private val deletePrefix = "delete from ${classInfo.table.name} where "

    fun <P1 : Any> createQuery(p1: Condition<P1>): OneParameterQuery<P1> =
        OneParameterQuery(p1)

    fun <P1 : Any, P2 : Any> createQuery(p1: Condition<P1>, p2: Condition<P2>):
            TwoParameterQuery<P1, P2> = TwoParameterQuery(p1, p2)

    fun <P1 : Any, P2 : Any, P3 : Any> createQuery(p1: Condition<P1>, p2: Condition<P2>, p3: Condition<P3>):
            ThreeParameterQuery<P1, P2, P3> = ThreeParameterQuery(p1, p2, p3)

    fun createQuery(queryString: String): Query<Entity> {
        return QueryImpl(queryString.toIndexedPlaceholders())
    }

    @Suppress("unused")
    data class Condition<Type>(val conditionString: String, val prop: KProperty1<*, *>)

    inner class OneParameterQuery<P1 : Any> internal constructor(p1: Condition<P1>) {
        private val query = QueryImpl(p1)
        fun with(p1: P1): QueryWithParameters =
            query.with(p1)
    }

    inner class TwoParameterQuery<P1 : Any, P2 : Any> internal constructor(
        p1: Condition<P1>,
        p2: Condition<P2>
    ) {
        private val query = QueryImpl(p1, p2)
        fun with(p1: P1, p2: P2): QueryWithParameters =
            query.with(p1, p2)
    }

    inner class ThreeParameterQuery<P1 : Any, P2 : Any, P3 : Any>(
        p1: Condition<P1>,
        p2: Condition<P2>,
        p3: Condition<P3>
    ) {
        private val query = QueryImpl(p1, p2, p3)
        fun with(p1: P1, p2: P2, p3: P3): QueryWithParameters =
            query.with(p1, p2, p3)
    }

    // internal api
    inner class QueryImpl(private val queryString: String) : Query<Entity> {
        internal constructor(vararg conditions: Condition<*>) :
                this(conditions.joinToString(separator = " and ") {
                    "${dbFieldNameForProperty[it.prop]} ${it.conditionString}"
                }.toIndexedPlaceholders())

        override fun with(vararg parameter: Any): QueryWithParameters {
            val parameterValues =
                parameter
                    // remove Unit parameters because conditions that have no parameters use it
                    .filter { it != Unit }
                    .flatMap {
                        if (it is Pair<*, *>)
                            sequenceOf(it.first as Any, it.second as Any)
                        else
                            sequenceOf(it)
                    }
            return QueryWithParameters(queryString, parameterValues)
        }
    }

    inner class QueryWithParameters(
        private val queryString: String,
        private val parameterValues: List<Any>
    ) {

        suspend fun find(
            connectionProvider: ConnectionProvider,
            fetchRelations: Set<KProperty1<*, Relation>> = setOf()
        ): List<Entity> {
            return findAndTransform(connectionProvider, fetchRelations) { it.toList(mutableListOf()) }
        }

        suspend fun <R> findAndTransform(
            connectionProvider: ConnectionProvider,
            fetchRelations: Set<KProperty1<*, Relation>> = setOf(),
            transform: suspend (Flow<Entity>) -> R
        ): R {
            return connectionProvider.withConnection { connection ->
                val queryResult = connection.executeSelect(
                    parameterValues,
                    selectPrefix + queryString
                )
                transform(resultMapper.mapQueryResult(queryResult, fetchRelations, connectionProvider))
            }
        }

        suspend fun findSingle(
            connectionProvider: ConnectionProvider,
            fetchRelations: Set<KProperty1<*, Relation>> = setOf()
        ): Entity =
            findAndTransform(connectionProvider, fetchRelations) { it.single() }

        suspend fun delete(connectionProvider: ConnectionProvider): Long =
            connectionProvider.withConnection { connection ->
                connection.executeSelect(
                    parameterValues,
                    deletePrefix + queryString
                ).rowsUpdated()
            }

        suspend fun findOrCreate(connectionProvider: ConnectionProvider, creator: () -> Entity): Entity {
            return connectionProvider.withConnection { connection ->
                val existing =
                    resultMapper.mapQueryResult(
                        connection.executeSelect(
                            parameterValues,
                            selectPrefix + queryString
                        ),
                        connectionProvider = connectionProvider
                    )
                        .singleOrNull()
                existing ?: repository.create(connectionProvider, creator())
            }
        }

        suspend fun createOrUpdate(connectionProvider: ConnectionProvider, entity: Entity): Entity {
            return connectionProvider.withConnection { connection ->

                val existing =
                    resultMapper.mapQueryResult(
                        connection.executeSelect(
                            parameterValues,
                            selectPrefix + queryString
                        ),
                        connectionProvider = connectionProvider
                    )
                        .singleOrNull()
                if (existing == null) {
                    repository.create(connectionProvider, entity)
                } else {
                    val updatedInstance = idHandler.assignId(entity, idProperty.get(existing) as PK)
                    repository.update(connectionProvider, updatedInstance)
                    updatedInstance
                }
            }
        }
    }
}

private suspend fun DBConnection.executeSelect(
    parameterValues: List<Any>,
    sql: String
): DBResult = createStatement(sql).execute(listOf(), parameterValues)
