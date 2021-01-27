package r2dbcfun.query

import io.r2dbc.spi.Connection
import io.r2dbc.spi.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.reactive.awaitSingle
import r2dbcfun.Repository
import r2dbcfun.RepositoryException
import r2dbcfun.ResultMapper
import r2dbcfun.util.toIndexedPlaceholders
import r2dbcfun.util.toSnakeCase
import java.time.LocalDate
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

public fun <T : Any, V> KProperty1<T, V>.isNull(): QueryFactory.Condition<Unit> =
    QueryFactory.isNullCondition(this)

public fun <T : Any> KProperty1<T, String?>.like(): QueryFactory.Condition<String> =
    QueryFactory.likeCondition(this)

public fun <T : Any, V : Any> KProperty1<T, V?>.isEqualTo(): QueryFactory.Condition<V> =
    QueryFactory.isEqualToCondition(this)

public fun <T : Any> KProperty1<T, LocalDate?>.between():
        QueryFactory.Condition<Pair<LocalDate, LocalDate>> =
    QueryFactory.Condition("between ? and ?", this)

public class QueryFactory<T : Any> internal constructor(
    kClass: KClass<T>,
    private val resultMapper: ResultMapper<T>,
    private val repository: Repository<T>
) {
    public companion object {
        public fun <T : Any, V> isNullCondition(property: KProperty1<T, V>): Condition<Unit> =
            Condition("is null", property)

        public fun <T : Any> likeCondition(property: KProperty1<T, String?>): Condition<String> =
            Condition("like(?)", property)

        public fun <T : Any, V : Any> isEqualToCondition(property: KProperty1<T, V?>):
                Condition<V> = Condition("=?", property)
    }

    private val snakeCaseForProperty =
        kClass.declaredMemberProperties.associateBy({ it }, { it.name.toSnakeCase() })

    @Suppress("SqlResolve")
    private val tableName = "${kClass.simpleName!!.toSnakeCase().toLowerCase()}s"

    private val selectPrefix =
        "select ${snakeCaseForProperty.values.joinToString { it }} from $tableName where "
    private val deletePrefix = "delete from $tableName where "

    public fun <P1 : Any> createQuery(p1: Condition<P1>): OneParameterQuery<P1> =
        OneParameterQuery(p1)

    public fun <P1 : Any, P2 : Any> createQuery(p1: Condition<P1>, p2: Condition<P2>):
            TwoParameterQuery<P1, P2> = TwoParameterQuery(p1, p2)

    public fun <P1 : Any, P2 : Any, P3 : Any> createQuery(p1: Condition<P1>, p2: Condition<P2>, p3: Condition<P3>):
            ThreeParameterQuery<P1, P2, P3> = ThreeParameterQuery(p1, p2, p3)


    @Suppress("unused")
    public data class Condition<Type>(val conditionString: String, val prop: KProperty1<*, *>)

    public inner class OneParameterQuery<P1 : Any> internal constructor(p1: Condition<P1>) {
        private val query = Query(p1)
        public fun with(connection: Connection, p1: P1): QueryWithParameters =
            query.with(connection, p1)
    }

    public inner class TwoParameterQuery<P1 : Any, P2 : Any> internal constructor(
        p1: Condition<P1>,
        p2: Condition<P2>
    ) {
        private val query = Query(p1, p2)
        public fun with(connection: Connection, p1: P1, p2: P2): QueryWithParameters =
            query.with(connection, p1, p2)
    }

    public inner class ThreeParameterQuery<P1 : Any, P2 : Any, P3 : Any>(
        p1: Condition<P1>,
        p2: Condition<P2>,
        p3: Condition<P3>
    ) {
        private val query = Query(p1, p2, p3)
        public  fun with(connection: Connection, p1: P1, p2: P2, p3: P3): QueryWithParameters =
            query.with(connection, p1, p2, p3)
    }


    // internal api
    public inner class Query internal constructor(vararg conditions: Condition<*>) {
        private val queryString =
            conditions.joinToString(separator = " and ") {
                "${snakeCaseForProperty[it.prop]} ${it.conditionString}"
            }.toIndexedPlaceholders()

        public fun with(connection: Connection, vararg parameter: Any): QueryWithParameters {
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

    public inner class QueryWithParameters(
        private val connection: Connection,
        private val queryString: String,
        private val parameterValues: Sequence<Any>
    ) {
        private suspend fun createStatement(
            parameterValues: Sequence<Any>,
            connection: Connection,
            sql: String
        ): Result {
            val statement = try {
                parameterValues.foldIndexed(connection.createStatement(sql))
                { idx, statement, property -> statement.bind(idx, property) }
            } catch (e: Exception) {
                throw RepositoryException("error creating statement for sql:$sql", e)
            }
            return try {
                statement.execute().awaitSingle()
            } catch (e: Exception) {
                throw RepositoryException("error executing select: $sql", e)
            }
        }

        public suspend fun find(): Flow<T> =
            resultMapper.findBy(createStatement(parameterValues, connection, selectPrefix + queryString))

        public suspend fun delete(): Int =
            createStatement(parameterValues, connection, deletePrefix + queryString).rowsUpdated.awaitSingle()

        public suspend fun findOrCreate(creator: () -> T): T {
            val existing = resultMapper.findBy(createStatement(parameterValues, connection, selectPrefix + queryString))
                .singleOrNull()
            return existing ?: repository.create(connection, creator())

        }

    }

}
