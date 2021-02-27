package r2dbcfun.query

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.reactive.awaitSingle
import r2dbcfun.ConnectionProvider
import r2dbcfun.Repository
import r2dbcfun.ResultMapper
import r2dbcfun.internal.IDHandler
import r2dbcfun.util.toIndexedPlaceholders
import r2dbcfun.util.toSnakeCase
import java.time.LocalDate
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

fun <T : Any, V> KProperty1<T, V>.isNull(): QueryFactory.Condition<Unit> =
    QueryFactory.isNullCondition(this)

fun <T : Any> KProperty1<T, String?>.like(): QueryFactory.Condition<String> =
    QueryFactory.likeCondition(this)

fun <T : Any, V : Any> KProperty1<T, V?>.isEqualTo(): QueryFactory.Condition<V> =
    QueryFactory.isEqualToCondition(this)

fun <T : Any> KProperty1<T, LocalDate?>.between():
        QueryFactory.Condition<Pair<LocalDate, LocalDate>> =
    QueryFactory.Condition("between ? and ?", this)

class QueryFactory<T : Any> internal constructor(
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

    @Suppress("SqlResolve")
    private val tableName = "${kClass.simpleName!!.toSnakeCase().toLowerCase()}s"

    private val selectPrefix =
        "select ${snakeCaseForProperty.values.joinToString { it }} from $tableName where "
    private val deletePrefix = "delete from $tableName where "

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
        private val connection: ConnectionProvider,
        private val queryString: String,
        private val parameterValues: Sequence<Any>
    ) {

        suspend fun find(): Flow<T> =
            resultMapper.mapQueryResult(
                connection.connection.executeSelect(
                    parameterValues,
                    selectPrefix + queryString
                )
            )

        suspend fun delete(): Int =
            connection.connection.executeSelect(
                parameterValues,
                deletePrefix + queryString
            ).rowsUpdated.awaitSingle()

        suspend fun findOrCreate(creator: () -> T): T {
            val existing =
                resultMapper.mapQueryResult(
                    connection.connection.executeSelect(
                        parameterValues,
                        selectPrefix + queryString
                    )
                )
                    .singleOrNull()
            return existing ?: repository.create(connection, creator())

        }

        suspend fun createOrUpdate(entity: T): T {
            val existing =
                resultMapper.mapQueryResult(
                    connection.connection.executeSelect(
                        parameterValues,
                        selectPrefix + queryString
                    )
                )
                    .singleOrNull()
            return if (existing == null) {
                repository.create(connection, entity)
            } else {
                val updatedInstance = idHandler.assignId(entity, idHandler.getId(idProperty.get(existing)))
                repository.update(connection, updatedInstance)
                updatedInstance
            }
        }

    }

}

