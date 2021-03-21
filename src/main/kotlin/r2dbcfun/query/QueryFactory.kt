package r2dbcfun.query

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.singleOrNull
import r2dbcfun.Repository
import r2dbcfun.ResultMapper
import r2dbcfun.dbio.ConnectionProvider
import r2dbcfun.dbio.DBConnection
import r2dbcfun.dbio.DBResult
import r2dbcfun.internal.IDHandler
import r2dbcfun.internal.Table
import r2dbcfun.util.toIndexedPlaceholders
import r2dbcfun.util.toSnakeCase
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

class QueryFactory<T : Any> internal constructor(
    val table: Table,
    kClass: KClass<T>,
    private val resultMapper: ResultMapper<T>,
    private val repository: Repository<T>,
    private val idHandler: IDHandler<T>,
    private val idProperty: KProperty1<T, Any>
) {
    companion object {
        fun <T : Any, V> isNullCondition(property: KProperty1<T, V>): Condition<Unit> =
            Condition("is null", property)

        fun <T : Any> likeCondition(property: KProperty1<T, String?>): Condition<String> =
            Condition("like(?)", property)

        fun <T : Any, V : Any> isEqualToCondition(property: KProperty1<T, V?>):
                Condition<V> = Condition("=?", property)
    }

    private val snakeCaseForProperty =
        kClass.declaredMemberProperties.associateBy({ it }, { it.name.toSnakeCase() })


    private val selectPrefix =
        "select ${snakeCaseForProperty.values.joinToString { it }} from ${table.name} where "
    private val deletePrefix = "delete from ${table.name} where "

    fun <P1 : Any> createQuery(p1: Condition<P1>): OneParameterQuery<P1> =
        OneParameterQuery(p1)

    fun <P1 : Any, P2 : Any> createQuery(p1: Condition<P1>, p2: Condition<P2>):
            TwoParameterQuery<P1, P2> = TwoParameterQuery(p1, p2)

    fun <P1 : Any, P2 : Any, P3 : Any> createQuery(p1: Condition<P1>, p2: Condition<P2>, p3: Condition<P3>):
            ThreeParameterQuery<P1, P2, P3> = ThreeParameterQuery(p1, p2, p3)


    @Suppress("unused")
    data class Condition<Type>(val conditionString: String, val prop: KProperty1<*, *>)

    inner class OneParameterQuery<P1 : Any> internal constructor(p1: Condition<P1>) {
        private val query = Query(p1)
        fun with(connection: ConnectionProvider, p1: P1): QueryWithParameters =
            query.with(connection, p1)
    }

    inner class TwoParameterQuery<P1 : Any, P2 : Any> internal constructor(
        p1: Condition<P1>,
        p2: Condition<P2>
    ) {
        private val query = Query(p1, p2)
        fun with(connection: ConnectionProvider, p1: P1, p2: P2): QueryWithParameters =
            query.with(connection, p1, p2)
    }

    inner class ThreeParameterQuery<P1 : Any, P2 : Any, P3 : Any>(
        p1: Condition<P1>,
        p2: Condition<P2>,
        p3: Condition<P3>
    ) {
        private val query = Query(p1, p2, p3)
        fun with(connection: ConnectionProvider, p1: P1, p2: P2, p3: P3): QueryWithParameters =
            query.with(connection, p1, p2, p3)
    }


    // internal api
    inner class Query internal constructor(vararg conditions: Condition<*>) {
        private val queryString =
            conditions.joinToString(separator = " and ") {
                "${snakeCaseForProperty[it.prop]} ${it.conditionString}"
            }.toIndexedPlaceholders()

        fun with(connection: ConnectionProvider, vararg parameter: Any): QueryWithParameters {
            val parameterValues =
                parameter.asSequence()
                    // remove Unit parameters because conditions that have no parameters use it
                    .filter { it != Unit }
                    .flatMap {
                        if (it is Pair<*, *>)
                            sequenceOf(it.first as Any, it.second as Any)
                        else
                            sequenceOf(it)
                    }
            return QueryWithParameters(connection, queryString, parameterValues)
        }
    }

    inner class QueryWithParameters(
        private val connectionProvider: ConnectionProvider,
        private val queryString: String,
        private val parameterValues: Sequence<Any>
    ) {

        suspend fun find(): Flow<T> =
            resultMapper.mapQueryResult(
                connectionProvider.withConnection { connection ->
                    connection.executeSelect(
                        parameterValues,
                        selectPrefix + queryString
                    )
                }
            )

        suspend fun delete(): Int =
            connectionProvider.withConnection { connection ->
                connection.executeSelect(
                    parameterValues,
                    deletePrefix + queryString
                ).rowsUpdated()
            }

        suspend fun findOrCreate(creator: () -> T): T {
            return connectionProvider.withConnection { connection ->
                val existing =
                    resultMapper.mapQueryResult(
                        connection.executeSelect(
                            parameterValues,
                            selectPrefix + queryString
                        )
                    )
                        .singleOrNull()
                existing ?: repository.create(connectionProvider, creator())
            }
        }

        suspend fun createOrUpdate(entity: T): T {
            return connectionProvider.withConnection { connection ->

                val existing =
                    resultMapper.mapQueryResult(
                        connection.executeSelect(
                            parameterValues,
                            selectPrefix + queryString
                        )
                    )
                        .singleOrNull()
                if (existing == null) {
                    repository.create(connectionProvider, entity)
                } else {
                    val updatedInstance = idHandler.assignId(entity, idHandler.getId(idProperty.get(existing)))
                    repository.update(connectionProvider, updatedInstance)
                    updatedInstance
                }
            }
        }

    }

}

private suspend fun DBConnection.executeSelect(
    parameterValues: Sequence<Any>,
    sql: String
): DBResult = createStatement(sql).execute(listOf(), parameterValues)

