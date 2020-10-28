package r2dbcfun.query

import io.r2dbc.spi.Connection
import kotlinx.coroutines.flow.Flow
import r2dbcfun.ClassInfo
import r2dbcfun.R2dbcRepo
import r2dbcfun.toSnakeCase
import java.time.LocalDate
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties


public fun <T : Any> KProperty1<T, String>.like(): Condition<String> = Condition<String>("like(?)", this)

public fun <T : Any> KProperty1<T, LocalDate>.between(): Condition<Pair<LocalDate, LocalDate>> =
    Condition<Pair<LocalDate, LocalDate>>("between ? and ?", this)

public data class Condition<Type>(val conditionString: String, val prop: KProperty1<*, *>)

public class TwoParameterQuery<T : Any, P1 : Any, P2 : Any>(kClass: KClass<T>, p1: Condition<P1>, p2: Condition<P2>) {
    internal val query = Query(kClass, p1, p2)
    public suspend fun find(connection: Connection, p1: P1, p2: P2): Flow<T> = query.find(connection, p1, p2)
}


// internal api
public open class Query<T : Any>(kClass: KClass<T>, private vararg val conditions: Condition<*>) {
    private val classInfo = ClassInfo(kClass)
    internal val snakeCaseForProperty =
        kClass.declaredMemberProperties.associateBy({ it }, { it.name.toSnakeCase() })

    @Suppress("SqlResolve")
    private val tableName = "${kClass.simpleName!!.toSnakeCase().toLowerCase()}s"

    internal val selectString = run {

        val queryString =
            conditions.joinToString(separator = " and ") { "${snakeCaseForProperty[it.prop]} ${it.conditionString}" }
        "select ${classInfo.fieldInfo.joinToString { it.snakeCaseName }} from $tableName where " + queryString
    }
    private val finder = R2dbcRepo(kClass).finder


    public suspend fun find(connection: Connection, vararg parameter: Any): Flow<T> =
        finder.findBy(parameter.asList(), connection, selectString)

}
